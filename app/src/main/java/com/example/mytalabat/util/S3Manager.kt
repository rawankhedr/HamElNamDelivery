package com.example.mytalabat.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList // <-- NEW IMPORT
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object S3Manager {
    // Replace these with your actual AWS credentials
    private const val ACCESS_KEY = ""
    private const val SECRET_KEY = ""
    private const val BUCKET_NAME = "hamelnam"
    private const val BUCKET_REGION = "eu-north-1" // Change to your bucket region

    private lateinit var s3Client: AmazonS3Client
    private lateinit var transferUtility: TransferUtility

    fun initialize(context: Context) {
        val credentials = BasicAWSCredentials(ACCESS_KEY, SECRET_KEY)
        s3Client = AmazonS3Client(credentials, Region.getRegion(Regions.fromName(BUCKET_REGION)))
        transferUtility = TransferUtility.builder()
            .context(context)
            .s3Client(s3Client)
            .build()
    }

    suspend fun uploadProfilePhoto(
        context: Context,
        userId: String,
        photoUri: Uri
    ): String = suspendCancellableCoroutine { continuation ->
        try {
            // Generate unique filename
            val timestamp = System.currentTimeMillis()
            val fileName = "profiles/$userId/profile_$timestamp.jpg"

            // Get file from URI
            val file = getFileFromUri(context, photoUri)

            // Upload to S3
            val uploadObserver: TransferObserver = transferUtility.upload(
                BUCKET_NAME,
                fileName,
                file,
                // --- FIX 1: Set PublicRead ACL to allow image loading from the public URL ---
                CannedAccessControlList.PublicRead
            )

            uploadObserver.setTransferListener(object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState?) {
                    when (state) {
                        TransferState.COMPLETED -> {
                            // Construct the S3 URL
                            val s3Url = "https://$BUCKET_NAME.s3.$BUCKET_REGION.amazonaws.com/$fileName"
                            Log.d("S3Manager", "Upload completed: $s3Url")

                            // Clean up temp file
                            file.delete()

                            if (continuation.isActive) {
                                continuation.resume(s3Url)
                            }
                        }
                        TransferState.FAILED -> {
                            Log.e("S3Manager", "Upload failed")
                            file.delete()
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    Exception("Failed to upload photo to S3")
                                )
                            }
                        }
                        TransferState.CANCELED -> {
                            file.delete()
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    Exception("Upload canceled")
                                )
                            }
                        }
                        else -> {
                            // IN_PROGRESS, WAITING, etc.
                            Log.d("S3Manager", "Upload state: $state")
                        }
                    }
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                    val percentage = ((bytesCurrent.toFloat() / bytesTotal.toFloat()) * 100).toInt()
                    Log.d("S3Manager", "Upload progress: $percentage%")
                }

                override fun onError(id: Int, ex: Exception?) {
                    Log.e("S3Manager", "Upload error", ex)
                    file.delete()
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            ex ?: Exception("Unknown upload error")
                        )
                    }
                }
            })

            continuation.invokeOnCancellation {
                uploadObserver.cleanTransferListener()
                file.delete()
            }

        } catch (e: Exception) {
            Log.e("S3Manager", "Error preparing upload", e)
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open file")

        val tempFile = File(context.cacheDir, "temp_upload_${UUID.randomUUID()}.jpg")
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()

        return tempFile
    }
}
