package com.febrivio.sumbercomplang.services

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import com.febrivio.sumbercomplang.network.ApiClient.ApiServiceAuth

/**
 * Helper class to handle downloading and saving PDF reports
 */
class PdfDownloadHelper(private val context: Context) {

    /**
     * Downloads and saves a monthly report
     *
     * @param month Month in format "MM" (e.g., "06")
     * @param year Year in format "YYYY" (e.g., "2025")
     * @param reportType Type of report (e.g., "kolam", "rental")
     * @param token Authentication token
     * @param onStartDownload Callback when download starts
     * @param onFinishDownload Callback when download completes
     * @param onError Callback when an error occurs
     */
    fun downloadMonthlyReport(
        month: String,
        year: String,
        reportType: String,
        token: String,
        onStartDownload: () -> Unit,
        onFinishDownload: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Notify download started
        onStartDownload()

        // Make API call
        ApiServiceAuth(context, token).downloadMonthlyReport(month, year, reportType)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        response.body()?.let { responseBody ->
                            // Start download in background
                            Thread {
                                try {
                                    // Generate appropriate prefix based on report type
                                    val filePrefix = "${reportType.capitalize()}_$month-$year"
                                    saveReportPDF(responseBody, filePrefix, reportType)
                                    onFinishDownload()
                                } catch (e: Exception) {
                                    onError("Gagal menyimpan file: ${e.message}")
                                }
                            }.start()
                        }
                    } else {
                        onError("Gagal mengunduh laporan: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    onError("Error: ${t.message}")
                }
            })
    }

    /**
     * Saves the PDF file to Downloads directory and opens it
     *
     * @param responseBody The PDF file data
     * @param filePrefix Prefix for the saved file name
     * @param reportType Type of report (for naming)
     */
    private fun saveReportPDF(responseBody: ResponseBody, filePrefix: String, reportType: String) {
        try {
            // Create file in Downloads directory
            val fileName = "Laporan_${reportType.capitalize()}_$filePrefix.pdf"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            // Write file to storage
            val inputStream = responseBody.byteStream()
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(4096)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Show success message and open file on UI thread
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(
                    context,
                    "Laporan berhasil disimpan di Downloads/$fileName",
                    Toast.LENGTH_LONG
                ).show()

                // Open the PDF file
                openPdfFile(file)
            }
        } catch (e: Exception) {
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "Error menyimpan file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Opens a PDF file using available PDF viewer app
     *
     * @param file The PDF file to open
     */
    private fun openPdfFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Tidak ada aplikasi untuk membuka PDF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}