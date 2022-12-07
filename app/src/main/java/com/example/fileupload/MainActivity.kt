package com.example.fileupload

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage


class MainActivity : ComponentActivity() {
    lateinit var storageRef : StorageReference
    private var listOfUrl = mutableStateListOf<Uri>()
    private var isLoading = mutableStateOf(false)


    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Button(onClick = {
                        //your onclick code here
                        val galleryIntent = Intent(Intent.ACTION_PICK)
                        // here item is type of image
                        galleryIntent.type = "image/*"
                        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        // ActivityResultLauncher callback
                        imagePickerActivityResult.launch(galleryIntent)
                    }) {
                        Text(text = "Select photos")
                    }
                }
                if (isLoading.value) {
                    ShowProgressBar()
                } else {
                    showList()
                }
            }


        }
        // creating a storage reference
        var storage = Firebase.storage
        // Create a storage reference from our app
        storageRef = storage.reference

    }

    private var imagePickerActivityResult : ActivityResultLauncher<Intent> =
    // lambda expression to receive a result back, here we
        // receive single item(photo) on selection
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result != null) {
                isLoading.value = true
                var imageCount = result.data?.clipData?.itemCount ?: 0

                var clipData : ClipData? = result.data?.clipData

                for (i in 0 until imageCount) {
                    // body of loop

                    val imageUri : Uri? = clipData?.getItemAt(i)?.uri

                    // val fileName = imageUri?.pathSegments?.last()

                    // extract the file name with extension
                    val sd = getFileName(applicationContext, imageUri!!)

                    Log.i("Image", "URL $sd")

                    // Upload Task with upload to directory 'file'
                    // and name of the file remains same
                    val uploadTask = storageRef.child("file/$sd").putFile(imageUri)

                    // On success, download the file URL and display it
                    uploadTask.addOnSuccessListener {
                        // using glide library to display the image
                        storageRef.child("file/$sd").downloadUrl.addOnSuccessListener {

                            listOfUrl.add(it)
                            isLoading.value = false

                            Log.e("Firebase", "download passed")
                        }.addOnFailureListener {
                            Log.e("Firebase", "Failed in downloading")
                        }
                    }.addOnFailureListener {
                        Log.e("Firebase", "Image Upload fail")
                    }
                }


            }
        }


    private fun getFileName(context : Context, uri : Uri) : String? {
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor.use {
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        return cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    }
                }
            }
        }
        return uri.path?.lastIndexOf('/')?.let { uri.path?.substring(it) }
    }

    @Composable
    fun ShowProgressBar() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
    }

    @Composable
    fun showList(
        modifier : Modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp)
    ) {
        LazyColumn(modifier = modifier) {
            itemsIndexed(listOfUrl) { index, item ->
                item?.let {
                    BookItem(
                        book = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 15.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun BookItem(
    book : Uri,
    modifier : Modifier = Modifier
) {

    Spacer(modifier = Modifier.padding(top = 10.dp))
    GlideImage(
        model = book,
        contentDescription = "",
        modifier = Modifier.padding(23.dp),
    )
}



