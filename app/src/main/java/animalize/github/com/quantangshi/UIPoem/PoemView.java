package animalize.github.com.quantangshi.UIPoem;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import animalize.github.com.quantangshi.Data.InfoItem;
import animalize.github.com.quantangshi.Data.PoemWrapper;
import animalize.github.com.quantangshi.Data.RawPoem;
import animalize.github.com.quantangshi.Data.Typeset;
import animalize.github.com.quantangshi.R;


public class PoemView extends LinearLayout implements View.OnClickListener {
    private PoemWrapper mPoemWrapper;
    private Typeset mTypeset = Typeset.getInstance();

    private int mChineseMode = 2;

    private LinearLayout mRoot;
    private TextView mId;
    private TextView mTitle;
    private TextView mAuthor;
    private TextView mText;
    private ScrollView mScroller;

    public PoemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_poem, this);

        mRoot = (LinearLayout) findViewById(R.id.root);

        mId = (TextView) findViewById(R.id.poem_id);
        mId.setOnClickListener(this);
        mTitle = (TextView) findViewById(R.id.poem_title);
        mAuthor = (TextView) findViewById(R.id.poem_author);
        mText = (TextView) findViewById(R.id.poem_text);
        mScroller = (ScrollView) findViewById(R.id.poem_scroller);

        setScreenOn();
    }

    public void setScreenOn() {
        setKeepScreenOn(mTypeset.isScreenOn());
    }

    public void setPoem(RawPoem poem, boolean showPoem) {
        boolean first = mPoemWrapper == null;
        mPoemWrapper = PoemWrapper.getPoemWrapper(poem, mTypeset.getLineBreak());

        if (first) {
            updateTypeset();
        }

        if (showPoem) {
            refreshPoem(true);
        }
    }

    public void setMode(int mode) {
        mChineseMode = mode;
        if (mPoemWrapper != null) {
            refreshPoem(false);
        }
    }

    public void setBackgroundIMG() {
        BitmapDrawable bitmapDrawable = mTypeset.getPoemBGDrawable();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mRoot.setBackground(bitmapDrawable);
        } else {
            mRoot.setBackgroundDrawable(bitmapDrawable);
        }
    }

    public Typeset getTypeset() {
        return mTypeset;
    }

    public InfoItem getInfoItem() {
        InfoItem item = new InfoItem(
                mPoemWrapper.getID(),
                mPoemWrapper.getTitle(mChineseMode),
                mPoemWrapper.getAuthor(mChineseMode));

        return item;
    }

    public void updateTypeset() {
        mPoemWrapper.setLineBreak(mTypeset.getLineBreak());

        setBackgroundIMG();

        mTitle.setMaxLines(mTypeset.getTitleLines());
        mTitle.setTextSize(mTypeset.getTitleSize());

        int temp = (int) (mTypeset.getTitleSize() * 0.618);
        mId.setTextSize(temp);
        mAuthor.setTextSize(temp);

        mText.setTextSize(mTypeset.getTextSize());
        mText.setLineSpacing(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                mTypeset.getLineSpace(),
                getResources().getDisplayMetrics()),
                1.0f);
        mText.setText(mPoemWrapper.getText(mChineseMode));
    }

    private void refreshPoem(boolean toTop) {
        mTitle.setText(mPoemWrapper.getTitle(mChineseMode));

        mId.setText("" + mPoemWrapper.getID());
        mAuthor.setText(mPoemWrapper.getAuthor(mChineseMode));

        if (mChineseMode == 0 || mChineseMode == 1) {
            mText.setText(mPoemWrapper.getText(mChineseMode));
        } else {
            ArrayList<PoemWrapper.CodepointPosition> lst = mPoemWrapper.getCodeList();
            SpannableString ss = new SpannableString(mPoemWrapper.getText(mChineseMode));
            for (final PoemWrapper.CodepointPosition p : lst) {
                MyClickableSpan clickable = new MyClickableSpan(
                        getContext(),
                        String.valueOf(Character.toChars(p.s_codepoint))
                );

                ss.setSpan(clickable,
                        p.begin, p.end,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            if (mText.getMovementMethod() == null) {
                mText.setMovementMethod(LinkMovementMethod.getInstance());
            }
            mText.setText(ss, TextView.BufferType.SPANNABLE);
        }

        if (toTop) {
            mScroller.scrollTo(0, 0);
        }

    }

    public float getYPosi() {
        return (float) mScroller.getScrollY() / mScroller.getHeight();
    }

    public void setYPosi(float posi) {
        if (posi != 0) {
            int t = (int) (mScroller.getHeight() * posi);
            mScroller.scrollTo(0, t);
        }
    }

    @Override
    public void onClick(View v) {
        StringBuilder sb = new StringBuilder();
        sb.append("标题：");
        sb.append(mPoemWrapper.getTitle(mChineseMode));
        sb.append('\n');

        sb.append("作者：");
        sb.append(mPoemWrapper.getAuthor(mChineseMode));
        sb.append('\n');

        sb.append("编号：");
        sb.append(mPoemWrapper.getID());
        sb.append("\n\n");

        sb.append(mPoemWrapper.getText(mChineseMode));

        ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, sb));

        Toast.makeText(getContext(),
                "已复制本诗到剪贴板",
                Toast.LENGTH_SHORT).show();
    }

    private static class MyClickableSpan extends ClickableSpan {
        private WeakReference<Context> weakRef;
        private String s;

        public MyClickableSpan(Context activity, String s) {
            this.weakRef = new WeakReference<>(activity);
            this.s = s;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setColor(Color.rgb(0x33, 0x33, 0x99));
            ds.setUnderlineText(false);
        }

        @Override
        public void onClick(View widget) {
            Context context = weakRef.get();
            if (context != null) {
                Toast t = Toast.makeText(context, s, Toast.LENGTH_SHORT);
                // 字体
                ViewGroup group = (ViewGroup) t.getView();
                TextView messageTextView = (TextView) group.getChildAt(0);
                messageTextView.setTextSize(40);
                // 居中
                t.setGravity(Gravity.CENTER, 0, 0);
                // 显示
                t.show();
            }
        }
    }
}
