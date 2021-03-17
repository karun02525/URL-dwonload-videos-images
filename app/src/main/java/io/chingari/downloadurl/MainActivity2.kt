package io.chingari.downloadurl

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.daasuu.mp4compose.FillMode
import com.daasuu.mp4compose.Rotation
import com.daasuu.mp4compose.composer.Mp4Composer
import com.daasuu.mp4compose.filter.GlFilterGroup
import com.daasuu.mp4compose.filter.GlMonochromeFilter
import com.daasuu.mp4compose.filter.GlVignetteFilter
import io.chingari.downloadurl.lib.Downloader
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity2 : AppCompatActivity() {

    lateinit var thumbView: View
    var fileOriginal = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val downloadUrl = "http://192.168.43.205:5000/uploads/b.mp4"
        Downloader.mInstance.download(downloadUrl, "chingari", { url, progress ->
            progressTextView.text = "$progress%"
            // progressView.show()
            progressView.progress = progress
        }, { url, file ->
            fileOriginal = file.absolutePath
            progressTextView.text = "path：${file.absolutePath}"
            // shareVideoWhatsApp(file.absolutePath)
        })


        btnWhatapp.setOnClickListener {
            shareVideoWhatsApp(fileOriginal)
        }

        pauseView.setOnClickListener {
            Downloader.mInstance.pauseDownload(downloadUrl)
        }

        resumeView.setOnClickListener {
            Log.d("TAGS", "***************downloadUrl path ********" + fileOriginal)
            Downloader.mInstance.resumeDownload(downloadUrl)

        }
        btnCompress.setOnClickListener {
            doCompress()
        }
    }

    fun shareVideoWhatsApp(absolutePath: String) {
        val uri: Uri = Uri.parse(absolutePath)
        val videoshare = Intent(Intent.ACTION_SEND)
        videoshare.type = "*/*"
        videoshare.setPackage("com.whatsapp")
        videoshare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        videoshare.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(videoshare)
    }


    private fun doCompress() {
        Mp4Composer(fileOriginal, "/storage/emulated/0/Movies/test_chingari.mp4")
                .videoBitrate(420000)
                .listener(object : Mp4Composer.Listener {
                    override fun onProgress(progress: Double) {
                        Log.d("TAGS", "onProgress = $progress")
                    }

                    override fun onCurrentWrittenVideoTime(timeUs: Long) {
                        Log.d("TAGS", "onCurrentWrittenVideoTime()" + timeUs)
                    }

                    override fun onCompleted() {
                        runOnUiThread {
                            Toast.makeText(applicationContext, "codec complete path =/storage/emulated/0/Movies/test_chingari.mp4", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCanceled() {
                        Log.d("TAGS", "onCanceled")
                    }

                    override fun onFailed(exception: Exception) {
                        Log.e("TAGS", "onFailed()", exception)
                    }
                })
                .start()

    }


}