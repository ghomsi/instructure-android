/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */


package com.instructure.canvasapi2.tests;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.models.AccountNotification;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AccountNotificationManager_Test {

    public static void getAccountNotificationForStudent(String parentId, String studentId, String accountNotificationId, StatusCallback<AccountNotification> callback) {

        Response response = new Response.Builder()
                .code(200)
                .message("todo")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "todo".getBytes()))
                .addHeader("content-type", "application/json")
                .build();

        AccountNotification item = new AccountNotification();

        retrofit2.Response<AccountNotification> response1 = retrofit2.Response.success(item, response);
        callback.onResponse(response1, new LinkHeaders(), ApiType.CACHE);
    }
}
