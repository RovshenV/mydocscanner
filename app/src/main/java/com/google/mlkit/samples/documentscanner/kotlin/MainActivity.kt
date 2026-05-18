package com.google.mlkit.samples.documentscanner.kotlin

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.mlkit.samples.documentscanner.R
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

class MainActivity : AppCompatActivity() {

  private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // Tarama sonucu döndüğünde ne olacağını tanımlıyoruz
    scannerLauncher = registerForActivityResult(StartIntentSenderForResult()) { result ->
      handleActivityResult(result)
    }

    // Yeni tasarımımızdaki büyük mavi butonu bulup tıklanma özelliği veriyoruz
    findViewById<Button>(R.id.btn_scan).setOnClickListener {
      startScan()
    }
  }

  private fun startScan() {
    // Tüm ayarları burada sabitliyoruz (FULL Mod ve Galeri Kapalı)
    val options = GmsDocumentScannerOptions.Builder()
      .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
      .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
      .setGalleryImportAllowed(false)
      .build()

    GmsDocumentScanning.getClient(options)
      .getStartScanIntent(this)
      .addOnSuccessListener { intentSender: IntentSender ->
        scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
      }
      .addOnFailureListener { e: Exception ->
        Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
      }
  }

  private fun handleActivityResult(activityResult: ActivityResult) {
    val resultCode = activityResult.resultCode
    val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)

    if (resultCode == Activity.RESULT_OK && result != null) {
      // Tarama başarılıysa direkt PDF paylaşım ekranını aç
      result.pdf?.uri?.path?.let { path ->
        val externalUri = FileProvider.getUriForFile(this, packageName + ".provider", File(path))
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
          putExtra(Intent.EXTRA_STREAM, externalUri)
          type = "application/pdf"
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "PDF'i Paylaş veya Kaydet"))
      } ?: run {
        Toast.makeText(this, "PDF oluşturulamadı", Toast.LENGTH_SHORT).show()
      }
    } else if (resultCode == Activity.RESULT_CANCELED) {
      // Kullanıcı vazgeçerse altta küçük bir mesaj göster
      Toast.makeText(this, "Tarama iptal edildi", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(this, "Bir hata oluştu", Toast.LENGTH_SHORT).show()
    }
  }
}
