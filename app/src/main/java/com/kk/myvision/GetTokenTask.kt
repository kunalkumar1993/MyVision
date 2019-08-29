package com.kk.myvision

import android.accounts.Account
import android.app.Activity
import android.os.AsyncTask
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import java.io.IOException

/**
 * Created by KwikPay Android Team Pune on 29-08-2019.
 * Copyright (c) 2019 KwikPay Limited. All rights reserved.
 */
class GetTokenTask(
    var mActivity: Activity,
    var mAccount: Account?,
    var mScope: String?,
    internal var mRequestCode: Int
): AsyncTask<Void, Void, Void>() {
/*
    internal var mActivity: Activity
    internal var mScope: String
    internal var mAccount: Account
    internal var mRequestCode: Int = 0
*/


    override fun doInBackground(vararg p0: Void?): Void? {
        try {
            val token = fetchToken()
            if (token != null) {
                (mActivity as MainActivity).onTokenReceived(token)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }
    @Throws(IOException::class)
    protected fun fetchToken(): String? {
        var accessToken: String
        try {
            accessToken = GoogleAuthUtil.getToken(mActivity, mAccount, mScope)
            GoogleAuthUtil.clearToken(mActivity, accessToken) // used to remove stale tokens.
            accessToken = GoogleAuthUtil.getToken(mActivity, mAccount, mScope)
            return accessToken
        } catch (userRecoverableException: UserRecoverableAuthException) {
            mActivity.startActivityForResult(userRecoverableException.intent, mRequestCode)
        } catch (fatalException: GoogleAuthException) {
            fatalException.printStackTrace()
        }

        return null
    }


}