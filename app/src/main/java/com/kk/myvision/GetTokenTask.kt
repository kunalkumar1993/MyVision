package com.kk.myvision

import android.accounts.Account
import android.app.Activity
import android.os.AsyncTask

/**
 * Created by KwikPay Android Team Pune on 29-08-2019.
 * Copyright (c) 2019 KwikPay Limited. All rights reserved.
 */
class GetTokenTask(): AsyncTask<Void, Void, Void>() {
    internal var mScope: String
    internal var mAccount: Account
    internal var mRequestCode: Int = 0


    override fun doInBackground(vararg p0: Void?): Void {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}