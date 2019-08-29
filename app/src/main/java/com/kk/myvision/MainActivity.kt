package com.kk.myvision

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap


import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.AccountPicker
import kotlinx.android.synthetic.main.activity_main.*

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
     }else if (requestCode == REQUEST_GALLERY_IMAGE && resultCode == RESULT_OK && data != null) {
         uploadImage(data.data)
     } else if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
         if (resultCode == RESULT_OK) {
             val email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
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
        dispatchTakePictureIntent()
    }

}
