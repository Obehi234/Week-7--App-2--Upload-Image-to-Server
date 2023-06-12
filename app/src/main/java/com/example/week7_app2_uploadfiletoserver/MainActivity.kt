package com.example.week7_app2_uploadfiletoserver

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity(), UploadRequestBody.UploadCallback {
    private var selectedImageUri: Uri? = null
    private lateinit var image_view: ImageView
    private lateinit var textView: TextView
    private lateinit var button_upload: Button
    private lateinit var layout_root: ConstraintLayout
    private lateinit var progress_bar: ProgressBar




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        image_view = findViewById(R.id.image_view)
        textView = findViewById(R.id.textView)
        textView.setOnClickListener {
            openImageChooser()
        }

        button_upload = findViewById(R.id.button_upload)
        button_upload.setOnClickListener {
            uploadImage()
        }

        layout_root = findViewById(R.id.layout_root)
        progress_bar = findViewById(R.id.progress_bar)




        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE)
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               uploadImage()
            } else {
                // Permission denied, show a message or disable the upload button
                layout_root.snackbar("Permission denied, cannot upload image")
            }
        }
    }




    private fun uploadImage() {
        if (selectedImageUri == null) {
            layout_root.snackbar("Select an Image First")
            return
        }

        try {
        val parcelFileDescriptor = contentResolver.openFileDescriptor(
            selectedImageUri!!, "r", null
        ) ?: return


        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
        val file = File(cacheDir, contentResolver.getFileName(selectedImageUri!!))
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        progress_bar.progress = 0

        val body = UploadRequestBody(file, "image", this)

        MyApi().uploadImage(
            MultipartBody.Part.createFormData(
                "image",
                file.name,
                body
            ),
            RequestBody.create("multipart/form-data".toMediaTypeOrNull(), "json")
        ).enqueue(object : Callback<UploadResponse> {

            override fun onResponse(
                call: Call<UploadResponse>,
                response: Response<UploadResponse>
            ) {

                    Log.d("File Upload", "File Upload Successful")
                    progress_bar.progress = 100
                    layout_root.snackbar(response.body()?.message.toString())

            }

            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                layout_root.snackbar(t.message!!)
                Log.d("File Upload", "File Upload Failed")
                progress_bar.progress = 0
            }

        })

    } catch (e: IOException) {
        e.printStackTrace()
        layout_root.snackbar("Error occured during file handling")
        Log.d("Upload Failed", "File Upload failed")
        progress_bar.progress = 0
    }
    }

    private fun openImageChooser() {
        Intent(Intent.ACTION_PICK).also {
            it.type = "image/*"
            val mimeTypes = arrayOf("image/jpeg", "image/png")
            it.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            startActivityForResult(it, REQUEST_CODE_IMAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_IMAGE -> {
                    selectedImageUri = data?.data
                    image_view.setImageURI(selectedImageUri)
                }
            }

        }
    }

    companion object {
        const val REQUEST_CODE_IMAGE = 101
        const val PERMISSION_REQUEST_CODE = 123
    }

    override fun onProgressUpdate(percentage: Int) {
        progress_bar.progress = percentage
    }
}

private fun ContentResolver.getFileName(selectedImageUri: Uri): String {
    var name = ""
    val returnCursor = this.query(selectedImageUri, null, null, null, null)
    if (returnCursor != null) {
        val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        name = returnCursor.getString(nameIndex)
        returnCursor.close()
    }

    return name

}

private fun View.snackbar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).also { snackbar ->
        snackbar.setAction("OK") {
            snackbar.dismiss()
        }
    }.show()

}
