package io.chingari.downloadurl

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_image.*


class ImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
;

        val myLogo = BitmapFactory.decodeResource(this.resources, R.drawable.pic)

        img.setImageBitmap(myLogo)

        imgCon.setImageBitmap(addWatermark(myLogo,"karun Kumar"))

    }
}