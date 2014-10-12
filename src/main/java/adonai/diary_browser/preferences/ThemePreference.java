package adonai.diary_browser.preferences;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

import java.util.Map;

import adonai.diary_browser.NetworkService;
import adonai.diary_browser.R;
import adonai.diary_browser.Utils;
import adonai.diary_browser.database.DatabaseHandler;
import adonai.diary_browser.theming.HotLayoutInflater;
import adonai.diary_browser.theming.HotTheme;

import static adonai.diary_browser.database.DatabaseHandler.ThemeField;

/**
 * Created by adonai on 09.10.14.
 */
public class ThemePreference extends DialogPreference {
    private Map<String, String> mCssMappings;

    @TargetApi(Build.VERSION_CODES.L)
    public ThemePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ThemePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ThemePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.L)
    public ThemePreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateDialogView() {
        final LinearLayout verticalContainer = new LinearLayout(getContext()); // global container
        verticalContainer.setOrientation(LinearLayout.VERTICAL);

        TextView cssColors = new TextView(getContext());
        cssColors.setTextSize(16);
        cssColors.setGravity(Gravity.CENTER_HORIZONTAL);
        cssColors.setTypeface(null, Typeface.BOLD);
        cssColors.setText(R.string.web_themes);
        verticalContainer.addView(cssColors);

        mCssMappings = NetworkService.getCssColors(getContext());
        for(final Map.Entry<String, String> pair : mCssMappings.entrySet()) {
            LinearLayout item = (LinearLayout) HotLayoutInflater.from(getContext()).inflate(R.layout.color_list_item, verticalContainer, false);
            TextView label = (TextView) item.findViewById(R.id.text_label);
            label.setText(pair.getKey());
            final View colorView = item.findViewById(R.id.color_view);
            final int originalColor = Color.parseColor(pair.getValue());
            colorView.setBackgroundColor(originalColor);
            colorView.setOnClickListener(new CssOnClickListener(originalColor, colorView, pair));
            verticalContainer.addView(item);
        }

        TextView interfaceColors = new TextView(getContext());
        interfaceColors.setTextSize(16);
        interfaceColors.setGravity(Gravity.CENTER_HORIZONTAL);
        interfaceColors.setTypeface(null, Typeface.BOLD);
        interfaceColors.setText(R.string.interface_themes);
        verticalContainer.addView(interfaceColors);

        final DatabaseHandler database = ((PreferencesScreen)getContext()).getDatabase();
        Cursor themeBindings = database.getThemesCursor();
        while (themeBindings.moveToNext()) {
            LinearLayout item = new LinearLayout(getContext());
            item.setOrientation(LinearLayout.HORIZONTAL);

            TextView label = new TextView(getContext());
            label.setText(themeBindings.getString(ThemeField.TITLE.ordinal()));
            item.addView(label);

            for(int columnIndex = 2; columnIndex < themeBindings.getColumnCount(); ++columnIndex) {
                if(!themeBindings.isNull(columnIndex)) {
                    final String type = themeBindings.getString(0);
                    switch (themeBindings.getType(columnIndex)) {
                        case Cursor.FIELD_TYPE_INTEGER:
                            final View colorView = new View(getContext());
                            final ThemeField field = ThemeField.valueOf(themeBindings.getColumnName(columnIndex));
                            final int originalColor = themeBindings.getInt(columnIndex);
                            colorView.setLayoutParams(new LinearLayout.LayoutParams((int) Utils.convertDpToPixel(25f, getContext()), (int) Utils.convertDpToPixel(25f, getContext())));
                            colorView.setBackgroundColor(originalColor);
                            item.setOnClickListener(new StyleOnClickListener(originalColor, colorView, database, type, field));
                            item.addView(colorView);
                            break;
                    }
                }
            }
        }
        themeBindings.close();

        return verticalContainer;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == DialogInterface.BUTTON_POSITIVE) {
            NetworkService.replaceCssColors(getContext(), mCssMappings);
        } else
            super.onClick(dialog, which);
    }

    private class CssOnClickListener implements View.OnClickListener {
        private final int originalColor;
        private final View colorView;
        private final Map.Entry<String, String> pair;

        public CssOnClickListener(int originalColor, View colorView, Map.Entry<String, String> pair) {
            this.originalColor = originalColor;
            this.colorView = colorView;
            this.pair = pair;
        }

        @Override
        public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            LinearLayout colorPickerView = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.color_picker, null, false);
            final ColorPicker cp = (ColorPicker) colorPickerView.findViewById(R.id.picker);
            final SaturationBar sBar = (SaturationBar) colorPickerView.findViewById(R.id.saturation_bar);
            final ValueBar vBar = (ValueBar) colorPickerView.findViewById(R.id.value_bar);
            //final OpacityBar opBar = (OpacityBar) colorPickerView.findViewById(R.id.opacitybar);
            cp.setColor(originalColor);
            //opBar.setColor(originalColor);
            sBar.setColor(originalColor);
            vBar.setColor(originalColor);
            //cp.addOpacityBar(opBar);
            cp.addSaturationBar(sBar);
            cp.addValueBar(vBar);
            builder.setView(colorPickerView).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    colorView.setBackgroundColor(cp.getColor());
                    pair.setValue(String.format("#%06X", 0xFFFFFF & cp.getColor()));
                }
            }).create().show();
        }
    }

    private class StyleOnClickListener implements View.OnClickListener {
        private final int originalColor;
        private final View colorView;
        private final DatabaseHandler database;
        private final String type;
        private final ThemeField field;

        public StyleOnClickListener(int originalColor, View colorView, DatabaseHandler database, String type, ThemeField field) {
            this.originalColor = originalColor;
            this.colorView = colorView;
            this.database = database;
            this.type = type;
            this.field = field;
        }

        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            LinearLayout colorPickerView = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.color_picker, null, false);
            final ColorPicker cp = (ColorPicker) colorPickerView.findViewById(R.id.picker);
            final SaturationBar sBar = (SaturationBar) colorPickerView.findViewById(R.id.saturation_bar);
            final ValueBar vBar = (ValueBar) colorPickerView.findViewById(R.id.value_bar);
            //final OpacityBar opBar = (OpacityBar) colorPickerView.findViewById(R.id.opacitybar);
            cp.setColor(originalColor);
            //opBar.setColor(originalColor);
            sBar.setColor(originalColor);
            vBar.setColor(originalColor);
            //cp.addOpacityBar(opBar);
            cp.addSaturationBar(sBar);
            cp.addValueBar(vBar);
            builder.setView(colorPickerView).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    colorView.setBackgroundColor(cp.getColor());
                    database.modifyThemeRow(type, field, cp.getColor());
                    HotTheme.setTheme();
                }
            }).create().show();
        }
    }
}