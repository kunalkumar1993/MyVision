package com.kk.myvision

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask


import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.AccountPicker
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.model.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private var accessToken: String? = null
    val REQUEST_IMAGE_CAPTURE = 1
    val REQUEST_GALLERY_IMAGE = 10
    val REQUEST_CODE_PICK_ACCOUNT = 11
    val REQUEST_ACCOUNT_AUTHORIZATION = 12
    val REQUEST_PERMISSIONS = 13
    private val LOG_TAG = "MainActivity"
    internal var mAccount: Account? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getPhoto()
    }

 override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
     if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
         val imageBitmap = data!!.extras!!.get("data") as Bitmap
         imageView.setImageBitmap(imageBitmap)
         callCloudVision(imageBitmap)
        // getAuthToken()
     }else if (requestCode == REQUEST_GALLERY_IMAGE && resultCode == RESULT_OK && data != null) {
         uploadImage(data.data)
     } else if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
         if (resultCode == RESULT_OK) {
             val email = data!!.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
             val am = AccountManager.get(this)
             val accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE)
             for (account in accounts) {
                 if (account.name == email) {
                     mAccount = account
                     break
                 }
             }
             getAuthToken()
         } else if (resultCode == RESULT_CANCELED) {
             Toast.makeText(this, "No Account Selected", Toast.LENGTH_SHORT)
                 .show()
         }
     } else if (requestCode == REQUEST_ACCOUNT_AUTHORIZATION) {
         if (resultCode == RESULT_OK) {
             val extra = data!!.getExtras()
             onTokenReceived(extra!!.getString("authtoken"))
         } else if (resultCode == RESULT_CANCELED) {
             Toast.makeText(this, "Authorization Failed", Toast.LENGTH_SHORT)
                 .show()
         }
     }

    }



fun getPhoto(){
    bt_photo.setOnClickListener{
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.GET_ACCOUNTS),
            REQUEST_PERMISSIONS);
        pickUserAccount()
    }
}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_PERMISSIONS->{
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getAuthToken()
                } else {
                    Toast.makeText(this@MainActivity, "Permission Denied!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    }


    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }


    }

    private fun getAuthToken() {
        val SCOPE = "oauth2:https://www.googleapis.com/auth/cloud-platform"
        if (mAccount == null) {
            pickUserAccount()
        } else {
            GetTokenTask(this@MainActivity, mAccount, SCOPE, REQUEST_ACCOUNT_AUTHORIZATION)
                .execute()
        }
    }
    private fun pickUserAccount() {
        val accountTypes = arrayOf(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE)
        val intent = AccountPicker.newChooseAccountIntent(
            null, null,
            accountTypes, false, null, null, null, null
        )
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT)
    }
    fun onTokenReceived(token: String?) {
        accessToken = token
      //  dispatchTakePictureIntent()
    //    launchImagePicker()
       this.runOnUiThread{
           showDailog(
               "Image"

           )
       }



    }
    fun uploadImage(uri: Uri?) {
        if (uri != null) {
            try {
                val bitmap = resizeBitmap(
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                )
                callCloudVision(bitmap)
                imageView.setImageBitmap(bitmap)
            } catch (e: IOException) {
                Log.e(LOG_TAG, e.message)
            }

        } else {
            Log.e(LOG_TAG, "Null image was returned.")
        }
    }
    fun resizeBitmap(bitmap: Bitmap): Bitmap {

        val maxDimension = 1024
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = maxDimension
        var resizedHeight = maxDimension

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension
            resizedWidth =
                (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension
            resizedHeight =
                (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension
            resizedWidth = maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }

    fun getBase64EncodedJpeg(bitmap: Bitmap): Image {
        val image = Image()
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()
        image.encodeContent(imageBytes)
        return image
    }

    @Throws(IOException::class)
    private fun callCloudVision(bitmap: Bitmap) {
        resultTextView.setText("Retrieving results from cloud")

        object : AsyncTask<Any, Void, String>() {
            override fun doInBackground(vararg params: Any): String {
                try {
                    val credential = GoogleCredential().setAccessToken(accessToken)
                    val httpTransport = AndroidHttp.newCompatibleTransport()
                    val jsonFactory = GsonFactory.getDefaultInstance()

                    val builder = Vision.Builder(httpTransport, jsonFactory, credential)
                    val vision = builder.build()

                    val featureList = ArrayList<Feature>()
                    val labelDetection = Feature()
                    labelDetection.type = "LABEL_DETECTION"
                    labelDetection.maxResults = 10
                    featureList.add(labelDetection)

                    val textDetection = Feature()
                    textDetection.type = "TEXT_DETECTION"
                    textDetection.maxResults = 10
                    featureList.add(textDetection)

                    val landmarkDetection = Feature()
                    landmarkDetection.type = "LANDMARK_DETECTION"
                    landmarkDetection.maxResults = 10
                    featureList.add(landmarkDetection)

                    val imageList = ArrayList<AnnotateImageRequest>()
                    val annotateImageRequest = AnnotateImageRequest()
                    val base64EncodedImage = getBase64EncodedJpeg(bitmap)
                    annotateImageRequest.image = base64EncodedImage
                    annotateImageRequest.features = featureList
                    imageList.add(annotateImageRequest)

                    val batchAnnotateImagesRequest = BatchAnnotateImagesRequest()
                    batchAnnotateImagesRequest.requests = imageList

                    val annotateRequest = vision.images().annotate(batchAnnotateImagesRequest)
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.disableGZipContent = true
                    Log.d(LOG_TAG, "sending request")

                    val response = annotateRequest.execute()
                    return convertResponseToString(response)

                } catch (e: GoogleJsonResponseException) {
                    Log.e(LOG_TAG, "Request failed: " + e.content)
                } catch (e: IOException) {
                    Log.d(LOG_TAG, "Request failed: " + e.message)
                }

                return "Cloud Vision API request failed."
            }

            override fun onPostExecute(result: String) {
                Log.e("API_RESULT",result)
                resultTextView.setText(result)
            }
        }.execute()
    }
    private fun convertResponseToString(response: BatchAnnotateImagesResponse): String {
        val message = StringBuilder("Results:\n\n")
        message.append("Labels:\n")
        val labels = response.responses[0].labelAnnotations
        if (labels != null) {
            for (label in labels) {
                message.append(
                    String.format(
                        Locale.getDefault(), "%.3f: %s",
                        label.score, label.description
                    )
                )
                message.append("\n")
            }
        } else {
            message.append("nothing\n")
        }

        message.append("Texts:\n")
        val texts = response.responses[0]
            .textAnnotations
        if (texts != null) {
            for (text in texts) {
                message.append(
                    String.format(
                        Locale.getDefault(), "%s: %s",
                        text.locale, text.description
                    )
                )
                message.append("\n")
            }
        } else {
            message.append("nothing\n")
        }

        message.append("Landmarks:\n")
        val landmarks = response.responses[0]
            .landmarkAnnotations
        if (landmarks != null) {
            for (landmark in landmarks) {
                message.append(
                    String.format(
                        Locale.getDefault(), "%.3f: %s",
                        landmark.score, landmark.description
                    )
                )
                message.append("\n")
            }
        } else {
            message.append("nothing\n")
        }

        return message.toString()
    }
    private fun launchImagePicker() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select an image"),
            REQUEST_GALLERY_IMAGE
        )
    }

    fun showDailog(
        title: String

    ) {
        val items = arrayOf<CharSequence>("Capture", "Gallery","Cancel")
        val builder = AlertDialog.Builder(this)
       // val dialog = builder.create()
        // builder.setTitle(title);
        builder.setItems(items) { dialog, item ->
            if (item == 0) {
                dispatchTakePictureIntent()

            } else if (item == 1) {
              launchImagePicker()
            } else if (item == 2) {
                dialog.dismiss()
            }
        }

       val dialog = builder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val wmlp = dialog.window!!.attributes

        wmlp.gravity = Gravity.CENTER
        wmlp.x = 100   //x position
        wmlp.y = 100   //y position

        dialog.show()
    }


}
