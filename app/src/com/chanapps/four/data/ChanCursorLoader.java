package com.chanapps.four.data;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.text.Html;
import android.util.Log;

import com.chanapps.four.activity.SettingsActivity;

public class ChanCursorLoader extends AsyncTaskLoader<Cursor> {

    private static final String TAG = ChanCursorLoader.class.getSimpleName();

    protected final ForceLoadContentObserver mObserver;

    protected Cursor mCursor;
    protected Context context;

    protected String boardName;
    protected long threadNo;

    protected static final String[] columns = {
            ChanHelper.POST_ID,
            ChanHelper.POST_BOARD_NAME,
            ChanHelper.POST_IMAGE_URL,
            ChanHelper.POST_SHORT_TEXT,
            ChanHelper.POST_TEXT,
            ChanHelper.POST_TN_W,
            ChanHelper.POST_TN_H,
            ChanHelper.POST_W,
            ChanHelper.POST_H
    };

    protected ChanCursorLoader(Context context) {
        super(context);
        mObserver = new ForceLoadContentObserver();
    }

    public ChanCursorLoader(Context context, String boardName, long threadNo) {
        this(context);
        this.context = context;
        this.boardName = boardName;
        this.threadNo = threadNo;
    }

    public ChanCursorLoader(Context context, String boardName) {
        this(context, boardName, 0);
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
    	Log.i(TAG, "loadInBackground");
        SharedPreferences prefs = context.getSharedPreferences(ChanHelper.PREF_NAME, 0);
        boolean hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        boolean hideTextOnlyPosts = prefs.getBoolean(SettingsActivity.PREF_HIDE_TEXT_ONLY_POSTS, false);
        Log.i("ChanCursorLoader", "prefs: " + hideAllText + " " + hideTextOnlyPosts);
    	if (threadNo == 0) {
    		// loading board from file
    		ChanBoard board = ChanFileStorage.loadBoardData(getContext(), boardName);
    		if (board != null) {
	    		MatrixCursor matrixCursor = new MatrixCursor(columns);
	    		for (ChanPost thread : board.threads) {
	    			if (thread.tn_w <= 0 || thread.tim == null) {
		    			matrixCursor.addRow(new Object[] {
			   					thread.no, boardName, "",
			   					getThreadText(thread), getPostText(thread), thread.tn_w, thread.tn_h, thread.w, thread.h});
	    			} else {
		    			matrixCursor.addRow(new Object[] {
			   					thread.no, boardName, "http://0.thumbs.4chan.org/" + board.link + "/thumb/" + thread.tim + "s.jpg",
			   					getThreadText(thread), getPostText(thread), thread.tn_w, thread.tn_h, thread.w, thread.h});
	    			}
	    		}
	    		if (board.threads.length > 0) {
	    			registerContentObserver(matrixCursor, mObserver);
	    		}
	    		return matrixCursor;
    		}
    	} else {
    		// loading thread from file
    		ChanThread thread = ChanFileStorage.loadThreadData(getContext(), boardName, threadNo);
    		if (thread != null) {
	    		MatrixCursor matrixCursor = new MatrixCursor(columns);
	    		for (ChanPost post : thread.posts) {
	    			if (post.tn_w <= 0 || post.tim == null) {
	    				matrixCursor.addRow(new Object[] {
	    						post.no, boardName, "",
	    						getThreadText(post), getPostText(post), post.tn_w, post.tn_h, post.w, post.h});
	    			} else {
	    				matrixCursor.addRow(new Object[] {
	    						post.no, boardName, "http://0.thumbs.4chan.org/" + thread.board + "/thumb/" + post.tim + "s.jpg",
	    						getThreadText(post), getPostText(post), post.tn_w, post.tn_h, post.w, post.h});
	    			}
	    		}
	    		if (thread.posts.length > 0) {
	    			registerContentObserver(matrixCursor, mObserver);
	    		}
	    		return matrixCursor;
    		}
        }
        return null;
    }

    private Object getPostText(ChanPost post) {
		String text = "";
		if (post.sub != null) {
			text += post.sub;
		}
		if (post.com != null) {
			text += " " + post.com;
		}
		text = Html.fromHtml(text).toString();
		if (post.fsize > 0) {
			if (text.length() > 0) {
				text += "\n";
			}
			int kbSize = (post.fsize / 1024) + 1;
			text += kbSize + "kB " + post.w + "x" + post.h + " " + post.ext;
		}
		return text;
	}

	private Object getThreadText(ChanPost thread) {
		String text = "";
		if (thread.sub != null) {
			text += thread.sub;
		}
		if (thread.com != null) {
			text += " " + thread.com;
		}
		text = Html.fromHtml(text).toString();
		if (text.length() > 22) {
			text = text.substring(0, 22) + "...";
		}
        if (thread.resto == 0) { // it's a thread, add thread stuff
            if (text.length() > 0) {
                text += "\n";
            }
            if (thread.replies <= 0) {
                text += "no replies yet";
            } else if (thread.images <= 0) {
                text += thread.replies + " posts but no image replies";
            } else {
                text += thread.replies + " posts and " + thread.images + " image replies";
            }
            if (thread.imagelimit == 1) {
                text += " (IL)";
            }
            if (thread.bumplimit == 1) {
                text += " (BL)";
    		}
        }
		return text;
	}

	/**
     * Registers an observer to get notifications from the content provider
     * when the cursor needs to be refreshed.
     */
    void registerContentObserver(Cursor cursor, ContentObserver observer) {
        cursor.registerContentObserver(mObserver);
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(Cursor cursor) {
		Log.i(TAG, "deliverResult isReset(): " + isReset());
        if (isReset()) {
            // An async query came in while the loader is stopped
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        Cursor oldCursor = mCursor;
        mCursor = cursor;

        if (isStarted()) {
            super.deliverResult(cursor);
        }

        if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
    	Log.i(TAG, "onStartLoading mCursor: " + mCursor);
        if (mCursor != null) {
            deliverResult(mCursor);
        }
        if (takeContentChanged() || mCursor == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
    	Log.i(TAG, "onStopLoading");
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(Cursor cursor) {
    	Log.i(TAG, "onCanceled cursor: " + cursor);
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        Log.i(TAG, "onReset cursor: " + mCursor);
        // Ensure the loader is stopped
        onStopLoading();

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = null;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.print(prefix); writer.print("boardName="); writer.println(boardName);
        writer.print(prefix); writer.print("mCursor="); writer.println(mCursor);
    }
}
