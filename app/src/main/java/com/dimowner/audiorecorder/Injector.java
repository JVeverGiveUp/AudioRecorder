/*
 * Copyright 2018 Dmitriy Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dimowner.audiorecorder;

import android.content.Context;

import com.dimowner.audiorecorder.data.FileRepository;
import com.dimowner.audiorecorder.data.FileRepositoryImpl;
import com.dimowner.audiorecorder.data.Prefs;
import com.dimowner.audiorecorder.data.database.LocalRepository;
import com.dimowner.audiorecorder.data.database.LocalRepositoryImpl;
import com.dimowner.audiorecorder.data.database.RecordsDataSource;
import com.dimowner.audiorecorder.ui.MainContract;
import com.dimowner.audiorecorder.ui.MainPresenter;
import com.dimowner.audiorecorder.ui.records.RecordsContract;
import com.dimowner.audiorecorder.ui.records.RecordsPresenter;

public class Injector {

	private Context context;

	private MainContract.UserActionsListener mainPresenter;
	private RecordsContract.UserActionsListener recordsPresenter;

	public Injector(Context context) {
		this.context = context;
	}

	public Prefs providePrefs() {
		return Prefs.getInstance(context);
	}

	public RecordsDataSource provideRecordsDataSource() {
		return RecordsDataSource.getInstance(context);
	}

	public FileRepository provideFileRepository() {
		return FileRepositoryImpl.getInstance(context, providePrefs());
	}

	public LocalRepository provideLocalRepository() {
		return LocalRepositoryImpl.getInstance(provideRecordsDataSource());
	}

	public MainContract.UserActionsListener provideMainPresenter() {
		if (mainPresenter == null) {
			mainPresenter = new MainPresenter(providePrefs(), provideFileRepository(), provideLocalRepository());
		}
		return mainPresenter;
	}

	public RecordsContract.UserActionsListener provideRecordPresenter() {
		if (recordsPresenter == null) {
			recordsPresenter = new RecordsPresenter(provideLocalRepository());
		}
		return recordsPresenter;
	}
}