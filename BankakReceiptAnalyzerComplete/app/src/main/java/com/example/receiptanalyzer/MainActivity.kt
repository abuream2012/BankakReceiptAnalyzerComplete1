
package com.example.receiptanalyzer

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    private val PICK_IMAGE = 100
    private lateinit var imageView: ImageView
    private lateinit var listView: ListView
    private lateinit var btnExport: Button
    private lateinit var btnClear: Button
    private lateinit var db: ReceiptDatabase
    private var receipts = mutableListOf<Receipt>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        listView = findViewById(R.id.listView)
        btnExport = findViewById(R.id.btnExport)
        btnClear = findViewById(R.id.btnClear)

        db = Room.databaseBuilder(applicationContext, ReceiptDatabase::class.java, "receipt-db").build()

        loadReceipts()

        findViewById<Button>(R.id.btnPick).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE)
        }

        btnClear.setOnClickListener {
            lifecycleScope.launch {
                db.receiptDao().clearAll()
                loadReceipts()
            }
        }

        btnExport.setOnClickListener {
            lifecycleScope.launch {
                val data = db.receiptDao().getAll()
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Receipts")
                val header = sheet.createRow(0)
                val titles = listOf("رقم العملية","اسم المرسل إليه","المبلغ","التعليق")
                titles.forEachIndexed { i, t -> header.createCell(i).setCellValue(t) }
                data.forEachIndexed { idx, r ->
                    val row = sheet.createRow(idx+1)
                    row.createCell(0).setCellValue(r.transactionId)
                    row.createCell(1).setCellValue(r.name)
                    row.createCell(2).setCellValue(r.amount)
                    row.createCell(3).setCellValue(r.note)
                }
                val file = File(getExternalFilesDir(null), "BankakReceipts.xlsx")
                FileOutputStream(file).use {
                    workbook.write(it)
                }
                workbook.close()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "تصدير إلى: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==PICK_IMAGE && resultCode== Activity.RESULT_OK){
            val uri: Uri = data!!.data!!
            val imageStream: InputStream? = contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(imageStream)
            imageView.setImageBitmap(bmp)
            val image = InputImage.fromBitmap(bmp,0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { vt ->
                    val text = vt.text
                    val id = Regex("(?<=رقم العملية\\s)\\d+").find(text)?.value?:"غير معروف"
                    val name = Regex("(?<=اسم المرسل اليه\\s)[\\p{Arabic} ]+").find(text)?.value?:"غير معروف"
                    val amt = Regex("(?<=المبلغ\\s)[\\d,.]+").find(text)?.value?:"0"
                    val note = Regex("(?<=التعليق\\s)[\\p{Arabic}A-Za-z0-9 ,.\\-\\/()]+").find(text)?.value?:""
                    lifecycleScope.launch {
                        db.receiptDao().insert(Receipt(0,id,name,amt,note))
                        loadReceipts()
                    }
                }
        }
    }

    private fun loadReceipts(){
        lifecycleScope.launch {
            receipts = db.receiptDao().getAll().toMutableList()
            runOnUiThread {
                listView.adapter = ArrayAdapter(this@MainActivity,android.R.layout.simple_list_item_1,
                    receipts.map { "${it.transactionId} | ${it.name} | ${it.amount} | ${it.note}" })
            }
        }
    }
}
