// Copyright 2026 Courville Software
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.archos.mediacenter.utils.videodb;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.MatrixCursor;

import com.archos.mediaprovider.video.VideoStore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** IndexHelper.addToWatchCount: reads the stored count, then writes it back with the delta added. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class IndexHelperWatchCountTest {

    private static final String COLUMN = VideoStore.Video.VideoColumns.ARCHOS_WATCH_COUNT;

    private Context mContext;
    private ContentResolver mResolver;

    @Before
    public void setUp() {
        mContext = mock(Context.class);
        mResolver = mock(ContentResolver.class);
        when(mContext.getContentResolver()).thenReturn(mResolver);
    }

    /** The video 7 is in the library with that count (null: the column holds a null); no row if absent. */
    private void stored(boolean inLibrary, Integer count) {
        MatrixCursor cursor = new MatrixCursor(new String[]{COLUMN});
        if (inLibrary) cursor.addRow(new Object[]{count});
        when(mResolver.query(eq(VideoStore.Video.Media.EXTERNAL_CONTENT_URI), any(String[].class),
                eq("_id = 7"), isNull(), isNull())).thenReturn(cursor);
    }

    private int written() {
        ArgumentCaptor<ContentValues> values = ArgumentCaptor.forClass(ContentValues.class);
        verify(mResolver).update(eq(VideoStore.Video.Media.EXTERNAL_CONTENT_URI), values.capture(), eq("_id = 7"), isNull());
        return values.getValue().getAsInteger(COLUMN);
    }

    @Test
    public void addsOneToTheStoredCount() {
        stored(true, 3);
        assertEquals(4, IndexHelper.addToWatchCount(mContext, 7, 1));
        assertEquals(4, written());
    }

    @Test
    public void aNullCountIsZero() {
        stored(true, null);
        assertEquals(1, IndexHelper.addToWatchCount(mContext, 7, 1));
        assertEquals(1, written());
    }

    @Test
    public void theCountNeverGoesBelowZero() {
        stored(true, 0);
        assertEquals(0, IndexHelper.addToWatchCount(mContext, 7, -1));
        assertEquals(0, written());
    }

    @Test
    public void aVideoNotInTheLibraryIsLeftAlone() {
        stored(false, null);
        assertEquals(-1, IndexHelper.addToWatchCount(mContext, 7, 1));
        verify(mResolver, never()).update(any(), any(), any(), any());
    }
}
