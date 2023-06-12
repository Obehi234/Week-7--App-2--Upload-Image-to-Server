package com.example.week7_app2_uploadfiletoserver

data class UploadResponse(
    val error: Boolean,
    val message: String,
    val image: String?
)
