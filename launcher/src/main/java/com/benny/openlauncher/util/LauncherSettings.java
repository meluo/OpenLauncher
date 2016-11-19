package com.benny.openlauncher.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.benny.openlauncher.widget.AppDrawer;
import com.benny.openlauncher.widget.Desktop;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LauncherSettings {

    private static LauncherSettings ourInstance;

    public static LauncherSettings getInstance(Context c) {
        return ourInstance == null ? ourInstance = new LauncherSettings(c) : ourInstance;
    }

    public List<List<Desktop.Item>> desktopData = new ArrayList<>();
    public List<Desktop.Item> dockData = new ArrayList<>();

    public GeneralSettings generalSettings;
    public SharedPreferences pref;
    public Context context;

    private static final String DesktopData2FileName = "desktopData2.json";
    private static final String DesktopDataFileName = "desktopData.json";
    private static final String DockDataFileName = "dockData.json";
    private static final String GeneralSettingsFileName = "generalSettings.json";

    private ArrayList<String> iconCacheIDs = new ArrayList<>();

    private LauncherSettings(Context c) {
        this.context = c;
        pref = c.getSharedPreferences("LauncherSettings",Context.MODE_PRIVATE);
        iconCacheIDs.clear();

        readSettings();
    }

    private void readDockData(Gson gson){
        dockData.clear();
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(context.openFileInput(DockDataFileName)));
            reader.beginArray();
            while (reader.hasNext()) {
                Desktop.SimpleItem item = gson.fromJson(reader,Desktop.SimpleItem.class);
                Desktop.Item item1 = new Desktop.Item(item);
                dockData.add(item1);

                //We get all the icon cache id
                if (item1.type == Desktop.Item.Type.SHORTCUT){
                    iconCacheIDs.add(item1.actions[0].getStringExtra("shortCutIconID"));
                }
                if (item1.type == Desktop.Item.Type.GROUP){
                    for (int i = 0; i < item1.actions.length; i++) {
                        String ID;
                        if ((ID = item1.actions[i].getStringExtra("shortCutIconID")) != null){
                            iconCacheIDs.add(ID);
                        }
                    }
                }
            }
            reader.endArray();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readDesktopData(Gson gson, Desktop.DesktopMode mode){
        desktopData.clear();
        String dataName = null;
        switch (mode){
            case Normal:
                dataName = DesktopDataFileName;
                break;
            case ShowAllApps:
                dataName = DesktopData2FileName;
                break;
        }
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(context.openFileInput(dataName)));
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginArray();
                ArrayList<Desktop.Item> items = new ArrayList<>();
                while (reader.hasNext()) {
                    Desktop.SimpleItem item = gson.fromJson(reader,Desktop.SimpleItem.class);
                    Desktop.Item item1 = new Desktop.Item(item);
                    items.add(item1);

                    //We get all the icon cache id
                    if (item.type == Desktop.Item.Type.SHORTCUT){
                        iconCacheIDs.add(item1.actions[0].getStringExtra("shortCutIconID"));
                    }
                    if (item1.type == Desktop.Item.Type.GROUP){
                        for (int i = 0; i < item1.actions.length; i++) {
                            String ID;
                            if ((ID = item1.actions[i].getStringExtra("shortCutIconID")) != null){
                                iconCacheIDs.add(ID);
                            }
                        }
                    }
                }
                desktopData.add(items);
                reader.endArray();
            }
            reader.endArray();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readSettings(){
        Gson gson = new Gson();

        String raw = Tool.getStringFromFile(GeneralSettingsFileName, context);
        if (raw == null)
            generalSettings = new GeneralSettings();
        else
            generalSettings = gson.fromJson(raw,GeneralSettings.class);

        readDockData(gson);
        readDesktopData(gson,generalSettings.desktopMode);
        Tool.checkForUnusedIconAndDelete(context, iconCacheIDs);
    }

    public void switchDesktopMode(Desktop.DesktopMode mode){
        generalSettings.desktopMode = mode;
        writeSettings();
        Gson gson = new Gson();
        readDesktopData(gson,mode);

        //We init all the apps to the desktop for the first time.
        if (mode == Desktop.DesktopMode.ShowAllApps && desktopData.size() == 0){
            int pageCount = 0;
            List<AppManager.App> apps = AppManager.getInstance(context).getApps();
            int appsSize = apps.size();
            while ((appsSize = appsSize - (generalSettings.desktopGridY * generalSettings.desktopGridX)) >= (generalSettings.desktopGridY * generalSettings.desktopGridX) || (appsSize > -(generalSettings.desktopGridY * generalSettings.desktopGridX))) {
                pageCount++;
            }
            Tool.print("Page count:"+pageCount);
            for (int i = 0; i < pageCount; i++) {
                ArrayList<Desktop.Item> items = new ArrayList<>();
                for (int x = 0; x < generalSettings.desktopGridX; x++) {
                    for (int y = 0; y < generalSettings.desktopGridY; y++) {
                        int pagePos = y * generalSettings.desktopGridY + x;
                        final int pos = generalSettings.desktopGridY * generalSettings.desktopGridX * i + pagePos;
                        if (pos >= apps.size())break;
                        Desktop.Item appItem = Desktop.Item.newAppItem(apps.get(pos));
                        appItem.x = x;
                        appItem.y = y;
                        items.add(appItem);
                    }
                }
                desktopData.add(items);
            }
        }
    }

    public Gson writeSettings(){
        Gson gson = new Gson();

        List<List<Desktop.SimpleItem>> simpleDesktopData = new ArrayList<>();
        List<Desktop.SimpleItem> simpleDockData = new ArrayList<>();

        for (Desktop.Item item:dockData) {
            simpleDockData.add(new Desktop.SimpleItem(item));
        }
        for (List<Desktop.Item> pages:desktopData) {
            final ArrayList<Desktop.SimpleItem> page = new ArrayList<>();
            simpleDesktopData.add(page);
            for (Desktop.Item item:pages) {
                page.add(new Desktop.SimpleItem(item));
            }
        }
        switch (generalSettings.desktopMode){
            case Normal:
                Tool.writeToFile(DesktopDataFileName,gson.toJson(simpleDesktopData), context);
                break;
            case ShowAllApps:
                Tool.writeToFile(DesktopData2FileName,gson.toJson(simpleDesktopData), context);
                break;
        }
        Tool.writeToFile(DockDataFileName,gson.toJson(simpleDockData), context);
        Tool.writeToFile(GeneralSettingsFileName,gson.toJson(generalSettings), context);

        return gson;
    }

    public static class GeneralSettings {
        //Icon
        public int iconSize = 58;
        public String iconPackName = "";

        //Desktop
        public Desktop.DesktopMode desktopMode = Desktop.DesktopMode.Normal;
        public int desktopPageCount = 1;
        public int desktopHomePage;
        public int desktopGridX = 4;
        public int desktopGridY = 4;
        public boolean desktopSearchBar = true;

        //Drawer
        public AppDrawer.DrawerMode drawerMode = AppDrawer.DrawerMode.Paged;
        public int drawerGridX = 4;
        public int drawerGridY = 5;
        public int drawerGridX_L = 5;
        public int drawerGridY_L = 3;
        public boolean drawerSearchBar = true;
        public boolean drawerRememberPage = true;

        //Dock
        public int dockGridX = 5;
        public boolean dockShowLabel = true;

        //MiniBar Arrangement
        public ArrayList<String> miniBarArrangement;

        //Not used
        public LauncherAction.Theme theme = LauncherAction.Theme.Light;
    }

}
