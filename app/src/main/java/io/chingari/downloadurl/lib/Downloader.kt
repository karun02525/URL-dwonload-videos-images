package io.chingari.downloadurl.lib

import android.os.Environment
import android.os.Handler
import android.util.Log
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class Downloader private constructor() {
    private val mDownloadTasks: ConcurrentHashMap<String, Task> by lazy {
        ConcurrentHashMap<String, Task>()
    }
    private val mOnDownloadHashMap: ConcurrentHashMap<String, Pair<OnDownload?, OnComplete?>> by lazy {
        ConcurrentHashMap<String, Pair<OnDownload?, OnComplete?>>()
    }
    private val mCalls: ConcurrentHashMap<String, Call> by lazy {
        ConcurrentHashMap<String, Call>()
    }
    private val mOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(TIME_OUT, TimeUnit.SECONDS)

            .build()
    }

    private val mHandler: Handler = Handler()

    companion object {
        private const val TAG: String = "Downloader"
        private const val TIME_OUT: Long = 5 * 60

        val mInstance: Downloader by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            Downloader()
        }


    }

    fun pauseDownload(url: String) {
        if(mDownloadTasks[url]?.status !=null) {
            mDownloadTasks[url]?.status = DownloadStatus.PAUSED
            mDownloadTasks[url]?.call!!.cancel()
        }
    }

    fun resumeDownload(taskUrl: String) {

        val task = mDownloadTasks[taskUrl]
        if (task == null) {
            Log.e(TAG, task + "is not exit")
            return
        }
        if (task.status != DownloadStatus.PAUSED){
            Log.e(TAG, taskUrl + "is already downloading")
            return
        }
        task.status = DownloadStatus.RESUME
        realDownload(task)
    }

    private fun String.validateMusicType(): Boolean {
        return this.validateType(".mp3")
                || this.validateType(".wav")
                || this.validateType(".aac")
                || this.validateType(".flac")
                || this.validateType(".ape")
    }

    private fun String.validateVideoType(): Boolean {

        return this.validateType(".mp4")
                || this.validateType(".3gp")
                || this.validateType(".avi")
                || this.validateType(".mkv")
                || this.validateType(".wmv")
                || this.validateType(".mpg")
                || this.validateType(".vob")
                || this.validateType(".flv")
                || this.validateType(".swf")
                || this.validateType(".mov")
                || this.validateType(",rmvb")
                || this.validateType(".mpeg4")
    }

    private fun String.validateType(fileType: String): Boolean {
        return this.endsWith(fileType, true)
    }

    private fun String.validatePicType(): Boolean {
        return this.validateType(".png")
                || this.validateType(".jpg")
                || this.validateType(".jpeg")
                || this.validateType(".gif")
                || this.validateType(".webp")

    }

    private fun String.getFileType(): String {
        return this.substring(this.lastIndexOf("."), this.lastIndex + 1)
    }

    fun multiDownload(
        taskUrls: MutableList<String>, filenames: MutableList<String>,
        onDownload: OnDownload? = null,
        onComplete: OnComplete? = null
    ) {
        if (taskUrls.isNullOrEmpty() || filenames.isNullOrEmpty()) {
            Log.e(TAG, "taskUrls or filenames can not be null or empty")
            return
        }

        if (taskUrls.size != filenames.size) {
            Log.e(TAG, "the size of taskUrls and filenames must be the same")
            return
        }
        taskUrls.forEachIndexed { index, taskUrl ->
            validateNeedCallback(taskUrl, onDownload, onComplete)
            download(taskUrl, filenames[index], onDownload)
        }


    }

    private fun validateNeedCallback(taskUrl: String, onDownload: OnDownload? = null, onComplete: OnComplete? = null) {
        if (onDownload == null && onComplete == null) {

        } else {
            val callback = Pair(onDownload, onComplete)
            mOnDownloadHashMap[taskUrl] = callback
        }

    }

    fun download(taskUrl: String, filename: String, onDownload: OnDownload? = null, onComplete: OnComplete? = null) {
        validateNeedCallback(taskUrl, onDownload, onComplete)
        val request = Request.Builder()
            .url(taskUrl)
            .build()
        val type: String = when {
            taskUrl.validateMusicType() -> Environment.DIRECTORY_MUSIC
            taskUrl.validateVideoType() -> Environment.DIRECTORY_MOVIES
            taskUrl.validatePicType() -> Environment.DIRECTORY_PICTURES
            else -> {
                Environment.DIRECTORY_DOWNLOADS
            }
        }
        val dir = Environment.getExternalStoragePublicDirectory(type)
        val mFile = File(dir.absolutePath, filename + taskUrl.getFileType())
        val task = Task(url = taskUrl, request = request, file = mFile)
        mDownloadTasks[taskUrl] = task
        realDownload(task)

    }

    private fun realDownload(mTask: Task) {

        mOkHttpClient.newCall(mTask.request).apply {
            mTask.call = this
        }.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

                mTask.errorMsg = if (e.message == null) {
                    "unKnow error"
                } else {
                    e.message
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful && response.code == 200) {
                    val body = response.body
                    if (body == null) {
                        mTask.errorMsg = response.message
                        return
                    }
                    mTask.contentSize = body.contentLength()
                    mTask.inputStream = body.byteStream()
                    if (mTask.fileOutputStream == null){
                        mTask.fileOutputStream = FileOutputStream(mTask.file)
                    }
                    calculate(mTask)
                } else {
                    Log.e(TAG, response.message)
                }

            }

        })
    }

    private fun calculate(mTask: Task) {
        val bytes = ByteArray(1024 * 4)
        val callback = mOnDownloadHashMap[mTask.url]
        try {
            while (true) {
                if (mTask.status == DownloadStatus.PAUSED) {
                    break
                }
                if (mTask.status == DownloadStatus.RESUME) {
                    mTask.inputStream!!.skip(mTask.hasDownloadSize)
                    mTask.status = DownloadStatus.DOWNLOADING
                }
                val len = mTask.inputStream!!.read(bytes)
                if (len == -1) {
                    mHandler.post {
                        callback?.second?.invoke(mTask.url,mTask.file)
                    }
                    mDownloadTasks.remove(mTask.url)
                    break
                }
                mTask.fileOutputStream!!.write(bytes, 0, len)
                mTask.hasDownloadSize += len
                mHandler.post {
                    callback?.first?.invoke(mTask.url, ((mTask.hasDownloadSize * 1f / mTask.contentSize) * 100).toInt())

                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.localizedMessage!!)
            mTask.inputStream!!.close()

        }


    }


}