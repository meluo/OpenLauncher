package com.benny.openlauncher.widget;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.Home;
import com.benny.openlauncher.util.AppManager;
import com.benny.openlauncher.viewutil.DesktopCallBack;
import com.benny.openlauncher.util.DragAction;
import com.benny.openlauncher.viewutil.GoodDragShadowBuilder;
import com.benny.openlauncher.viewutil.GroupIconDrawable;
import com.benny.openlauncher.viewutil.ItemViewFactory;
import com.benny.openlauncher.util.LauncherSettings;
import com.benny.openlauncher.util.Tool;

public class GroupPopupView extends FrameLayout {

    private CardView popupParent;
    private CellContainer cellContainer;
    private TextView title;

    private boolean init = false;

    private PopupWindow.OnDismissListener dismissListener;

    public GroupPopupView(Context context) {
        super(context);
        init();
    }

    public GroupPopupView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        if (isInEditMode()) return;
        init = false;

        bringToFront();
        popupParent = (CardView) LayoutInflater.from(getContext()).inflate(R.layout.view_grouppopup, this, false);
        cellContainer = (CellContainer) popupParent.findViewById(R.id.cc);
        title = (TextView) popupParent.findViewById(R.id.tv);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dismissListener != null)
                    dismissListener.onDismiss();
                setVisibility(View.INVISIBLE);
                dismissPopup();
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!init) {
            init = true;
            setVisibility(View.INVISIBLE);
        }
    }

    public void dismissPopup() {
        removeAllViews();
        dismissListener.onDismiss();
        cellContainer.removeAllViews();
        setVisibility(View.INVISIBLE);
    }

    public boolean showWindowV(final Desktop.Item item, final View itemView, final DesktopCallBack callBack) {
        if (getVisibility() == View.VISIBLE) return false;

        setVisibility(View.VISIBLE);
        popupParent.setVisibility(View.VISIBLE);
        final Context c = itemView.getContext();

        int[] cellSize = GroupPopupView.GroupDef.getCellSize(item.actions.length);
        cellContainer.setGridSize(cellSize[0], cellSize[1]);

        int iconSize = Tool.dp2px(LauncherSettings.getInstance(c).generalSettings.iconSize, c);
        int textHeight = Tool.dp2px(22, c);

        int contentPadding = Tool.dp2px(5, c);

        for (int x2 = 0; x2 < cellSize[0]; x2++) {
            for (int y2 = 0; y2 < cellSize[1]; y2++) {
                if (y2 * cellSize[0] + x2 > item.actions.length - 1) continue;
                final Intent act = item.actions[y2 * cellSize[0] + x2];
                AppItemView.Builder b = new AppItemView.Builder(getContext()).withOnTouchGetPosition();
                if (act.getStringExtra("shortCutIconID") != null) {
                    b.setShortcutItem(act);
                } else {
                    AppManager.App app = AppManager.getInstance(c).findApp(act.getComponent().getPackageName(), act.getComponent().getClassName());
                    if (app != null)
                    b.setAppItem(app);
                }
                final AppItemView view = b.getView();

                view.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view2) {
                        removeItem(callBack, item, act, (AppItemView) itemView, c);

                        itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        Intent i = new Intent();

                        if (act.getStringExtra("shortCutIconID") == null) {
                            i.putExtra("mDragData", Desktop.Item.newAppItem(AppManager.getInstance(c).findApp(act.getComponent().getPackageName(), act.getComponent().getClassName())));
                            ClipData data = ClipData.newIntent("mDragIntent", i);
                            itemView.startDrag(data, new GoodDragShadowBuilder(view), new DragAction(DragAction.Action.ACTION_APP), 0);
                        } else {
                            i.putExtra("mDragData", Desktop.Item.newShortcutItem(act));
                            ClipData data = ClipData.newIntent("mDragIntent", i);
                            itemView.startDrag(data, new GoodDragShadowBuilder(view), new DragAction(DragAction.Action.ACTION_SHORTCUT), 0);
                        }
                        dismissPopup();
                        return true;
                    }
                });
                if (!view.isShortcut) {
                    final AppManager.App app = AppManager.getInstance(c).findApp(act.getComponent().getPackageName(), act.getComponent().getClassName());
                    if (app == null){
                        removeItem(callBack, item, act, (AppItemView) itemView, c);
                    }else {
                    view.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Tool.createScaleInScaleOutAnim(view, new Runnable() {
                                @Override
                                public void run() {
                                    dismissPopup();
                                    setVisibility(View.INVISIBLE);
                                    Tool.startApp(c, app);
                                }
                            });
                        }
                    });}
                } else {
                    view.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Tool.createScaleInScaleOutAnim(view, new Runnable() {
                                @Override
                                public void run() {
                                    dismissPopup();
                                    setVisibility(View.INVISIBLE);
                                    view.getContext().startActivity(act);
                                }
                            });
                        }
                    });
                }
                cellContainer.addViewToGrid(view, x2, y2, 1, 1);
            }
        }
        title.setText(item.name);
        title.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Tool.askForText("Rename", item.name, getContext(), new Tool.OnTextGotListener() {
                    @Override
                    public void hereIsTheText(String str) {
                        if (str.isEmpty()) return;
                        callBack.removeItemFromSettings(item);
                        item.name = str;
                        callBack.addItemToSettings(item);

                        title.setText(str);
                    }
                });
            }
        });

        dismissListener = new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                AppItemView otv = ((AppItemView) itemView);
                if (!otv.getLabel().isEmpty())
                    otv.setLabel(title.getText().toString());
                if (((AppItemView) itemView).getIcon() instanceof  GroupIconDrawable)
                    ((GroupIconDrawable) ((AppItemView) itemView).getIcon()).popBack();
            }
        };

        int popupWidth = contentPadding * 4 + popupParent.getContentPaddingLeft() + popupParent.getContentPaddingRight() + (iconSize) * cellSize[0];
        popupParent.getLayoutParams().width = popupWidth;
        int popupHeight = contentPadding * 2 + popupParent.getContentPaddingTop() + popupParent.getContentPaddingBottom() + Tool.dp2px(30, c)
                + (iconSize + textHeight) * cellSize[1];
        popupParent.getLayoutParams().height = popupHeight;

        int[] coord = new int[2];
        itemView.getLocationInWindow(coord);

        coord[0] += itemView.getWidth() / 2;
        coord[1] += itemView.getHeight() / 2;

        coord[0] -= popupWidth / 2;
        coord[1] -= popupHeight / 2;

        int width = getWidth();
        int height = getHeight();

        if (coord[0] + popupWidth > width) {
            coord[0] += width - (coord[0] + popupWidth);
        }
        if (coord[1] + popupHeight > height) {
            coord[1] += height - (coord[1] + popupHeight);
        }
        if (coord[0] < 0) {
            coord[0] -= itemView.getWidth() / 2;
            coord[0] += popupWidth / 2;
        }
        if (coord[1] < 0) {
            coord[1] -= itemView.getHeight() / 2;
            coord[1] += popupHeight / 2;
        }

        popupParent.setPivotX(0);
        popupParent.setPivotX(0);
        popupParent.setX(coord[0]);
        popupParent.setY(coord[1]);

        addView(popupParent);
        return true;
    }

    private void removeItem(final DesktopCallBack callBack, final Desktop.Item item, Intent act, AppItemView itemView, Context c) {
        callBack.removeItemFromSettings(item);
        item.removeActions(act);
        if (item.actions.length == 1){
            item.type = Desktop.Item.Type.APP;
            AppItemView.Builder builder = new AppItemView.Builder(itemView);
            final AppManager.App app = AppManager.getInstance(itemView.getContext()).findApp(item.actions[0].getComponent().getPackageName(), item.actions[0].getComponent().getClassName());
            if (app != null) {
                builder.setAppItem(app).withOnClickLaunchApp(app).withOnLongPressDrag(item, DragAction.Action.ACTION_APP, new AppItemView.Builder.LongPressCallBack() {
                    @Override
                    public boolean readyForDrag(View view) {
                        return true;
                    }

                    @Override
                    public void afterDrag(View view) {
                        callBack.setLastItem(item, view);
                    }
                });
            }
            Home.launcher.desktop.requestLayout();
        }else {
            itemView.setIcon(ItemViewFactory.getGroupIconDrawable(c, item), false);
        }
        callBack.addItemToSettings(item);
    }

    public static class GroupDef {
        public static int maxItem = 12;

        public static int[] getCellSize(int count) {
            if (count <= 1)
                return new int[]{1, 1};
            if (count <= 2)
                return new int[]{2, 1};
            if (count <= 4)
                return new int[]{2, 2};
            if (count <= 6)
                return new int[]{3, 2};
            if (count <= 9)
                return new int[]{3, 3};
            if (count <= 12)
                return new int[]{4, 3};

            return new int[]{0, 0};
        }
    }
}
