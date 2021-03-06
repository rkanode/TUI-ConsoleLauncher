package ohi.andre.consolelauncher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.specific.RedirectCommand;
import ohi.andre.consolelauncher.managers.SkinManager;
import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.suggestions.SuggestionRunnable;
import ohi.andre.consolelauncher.managers.suggestions.SuggestionsManager;
import ohi.andre.consolelauncher.tuils.Sequence;
import ohi.andre.consolelauncher.tuils.StoppableThread;
import ohi.andre.consolelauncher.tuils.TimeManager;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.Hintable;
import ohi.andre.consolelauncher.tuils.interfaces.OnBatteryUpdate;
import ohi.andre.consolelauncher.tuils.interfaces.OnRedirectionListener;
import ohi.andre.consolelauncher.tuils.interfaces.Rooter;
import ohi.andre.consolelauncher.tuils.interfaces.SuggestionViewDecorer;
import ohi.andre.consolelauncher.tuils.stuff.PolicyReceiver;
import ohi.andre.consolelauncher.tuils.stuff.TrashInterfaces;

public class UIManager implements OnTouchListener {

    private final int RAM_DELAY = 3000;
    private final int TIME_DELAY = 1000;
    private final int STORAGE_DELAY = 60 * 1000;

    protected Context mContext;

    private SkinManager skinManager;

    private DevicePolicyManager policy;
    private ComponentName component;
    private GestureDetector det;
    private MainPack info;

    private InputMethodManager imm;
    private CommandExecuter trigger;
    private TerminalManager mTerminalAdapter;

    private TextView ram;
    private TextView device;
    private TextView battery;
    private TextView time;
    private TextView storage;

    int mediumPercentage, lowPercentage;
    String batteryFormat;
//    boolean batteryCharging;

    private String multipleCmdSeparator;

    private boolean selectFirstSuggestionEnter = false;
    private OnNewInputListener inputListener = new OnNewInputListener() {
        @Override
        public void onNewInput(String input) {
            if(suggestionsView != null) {
//                if(suggestionsView.getChildCount() > 0 && selectFirstSuggestionEnter) {
//                    View v = suggestionsView.getChildAt(0);
//                    v.performClick();
//                    return;
//                }
                suggestionsView.removeAllViews();

            }
            trigger.exec(input);

        }
    };

    private OnBatteryUpdate batteryUpdate = new OnBatteryUpdate() {
        @Override
        public void update(float p) {
            int percentage = (int) p;

            if(skinManager.manyColorsBattery) {
                if(percentage > mediumPercentage) battery.setTextColor(skinManager.battery_color_high);
                else if(percentage > lowPercentage) battery.setTextColor(skinManager.battery_color_medium);
                else battery.setTextColor(skinManager.battery_color_low);
            } else {
                battery.setTextColor(skinManager.battery_color_high);
            }

            if(batteryFormat == null) batteryFormat = XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.battery_format);

            String cp = batteryFormat.replaceAll("%[vV]", String.valueOf(percentage)).replaceAll("%[nN]", Tuils.NEWLINE);
            battery.setText(cp);
        }
    };

    private final String INT_AV = "%iav";
    private final String INT_TOT = "%itot";
    private final String EXT_AV = "%eav";
    private final String EXT_TOT = "%etot";

    private List<Pattern> storagePatterns;
    private String storageFormat;
    private Runnable storageRunnable = new Runnable() {
        @Override
        public void run() {
            if(storageFormat == null) storageFormat = XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.storage_format);

            if(storagePatterns == null) {
                storagePatterns = new ArrayList<>();

                storagePatterns.add(Pattern.compile(INT_AV + "tb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(INT_AV + "gb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(INT_AV + "mb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(INT_AV + "kb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(INT_AV + "b", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(INT_AV + "%", Pattern.CASE_INSENSITIVE));

                storagePatterns.add(Pattern.compile(INT_TOT + "tb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(INT_TOT + "gb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(INT_TOT + "mb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(INT_TOT + "kb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(INT_TOT + "b", Pattern.CASE_INSENSITIVE));

                storagePatterns.add(Pattern.compile(EXT_AV + "tb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(EXT_AV + "gb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(EXT_AV + "mb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(EXT_AV + "kb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(EXT_AV + "b", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(EXT_AV + "%", Pattern.CASE_INSENSITIVE));

                storagePatterns.add(Pattern.compile(EXT_TOT + "tb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(EXT_TOT + "gb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(EXT_TOT + "mb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(EXT_TOT + "kb", Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(EXT_TOT + "b", Pattern.CASE_INSENSITIVE));

                storagePatterns.add(Pattern.compile("%n", Pattern.CASE_INSENSITIVE));

                storagePatterns.add(Pattern.compile(INT_AV, Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(INT_TOT, Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(EXT_AV, Pattern.CASE_INSENSITIVE));
                storagePatterns.add(Pattern.compile(EXT_TOT, Pattern.CASE_INSENSITIVE));
            }

            double iav = Tuils.getAvailableInternalMemorySize(Tuils.BYTE);
            double itot = Tuils.getTotalInternalMemorySize(Tuils.BYTE);
            double eav = Tuils.getAvailableExternalMemorySize(Tuils.BYTE);
            double etot = Tuils.getTotalExternalMemorySize(Tuils.BYTE);

            String copy = storageFormat;

            copy = storagePatterns.get(0).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) iav, Tuils.TERA)));
            copy = storagePatterns.get(1).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) iav, Tuils.GIGA)));
            copy = storagePatterns.get(2).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) iav, Tuils.MEGA)));
            copy = storagePatterns.get(3).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) iav, Tuils.KILO)));
            copy = storagePatterns.get(4).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) iav, Tuils.BYTE)));
            copy = storagePatterns.get(5).matcher(copy).replaceAll(String.valueOf(Tuils.percentage(iav, itot)));

            copy = storagePatterns.get(6).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) itot, Tuils.TERA)));
            copy = storagePatterns.get(7).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) itot, Tuils.GIGA)));
            copy = storagePatterns.get(8).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) itot, Tuils.MEGA)));
            copy = storagePatterns.get(9).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) itot, Tuils.KILO)));
            copy = storagePatterns.get(10).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) itot, Tuils.BYTE)));

            copy = storagePatterns.get(11).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) eav, Tuils.TERA)));
            copy = storagePatterns.get(12).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) eav, Tuils.GIGA)));
            copy = storagePatterns.get(14).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) eav, Tuils.MEGA)));
            copy = storagePatterns.get(14).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) eav, Tuils.KILO)));
            copy = storagePatterns.get(15).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) eav, Tuils.BYTE)));
            copy = storagePatterns.get(16).matcher(copy).replaceAll(String.valueOf(Tuils.percentage(eav, etot)));

            copy = storagePatterns.get(17).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) etot, Tuils.TERA)));
            copy = storagePatterns.get(18).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) etot, Tuils.GIGA)));
            copy = storagePatterns.get(19).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) etot, Tuils.MEGA)));
            copy = storagePatterns.get(20).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) etot, Tuils.KILO)));
            copy = storagePatterns.get(21).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) etot, Tuils.BYTE)));

            copy = storagePatterns.get(22).matcher(copy).replaceAll(Tuils.NEWLINE);

            copy = storagePatterns.get(23).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) iav, Tuils.GIGA)));
            copy = storagePatterns.get(24).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) itot, Tuils.GIGA)));
            copy = storagePatterns.get(25).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) eav, Tuils.GIGA)));
            copy = storagePatterns.get(26).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) etot, Tuils.GIGA)));

            storage.setText(copy);
            storage.postDelayed(this, STORAGE_DELAY);
        }
    };

    private Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            time.setText(TimeManager.replace("%t0"));
            time.postDelayed(this, TIME_DELAY);
        }
    };

    private ActivityManager.MemoryInfo memory;
    private ActivityManager activityManager;

    private final String AV = "%av";
    private final String TOT = "%tot";

    List<Pattern> ramPatterns;
    String ramFormat;
    private Runnable ramRunnable = new Runnable() {
        @Override
        public void run() {
            if(ramFormat == null) ramFormat = XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.ram_format);

            if(ramPatterns == null) {
                ramPatterns = new ArrayList<>();

                ramPatterns.add(Pattern.compile(AV + "tb", Pattern.CASE_INSENSITIVE));
                ramPatterns.add(Pattern.compile(AV + "gb", Pattern.CASE_INSENSITIVE));
                ramPatterns.add(Pattern.compile(AV + "mb", Pattern.CASE_INSENSITIVE));
                ramPatterns.add(Pattern.compile(AV + "kb", Pattern.CASE_INSENSITIVE));
                ramPatterns.add(Pattern.compile(AV + "b", Pattern.CASE_INSENSITIVE));
                ramPatterns.add(Pattern.compile(AV + "%", Pattern.CASE_INSENSITIVE));

                ramPatterns.add(Pattern.compile(TOT + "tb", Pattern.CASE_INSENSITIVE));
                ramPatterns.add(Pattern.compile(TOT + "gb", Pattern.CASE_INSENSITIVE));
                ramPatterns.add(Pattern.compile(TOT + "mb", Pattern.CASE_INSENSITIVE));
                ramPatterns.add(Pattern.compile(TOT + "kb", Pattern.CASE_INSENSITIVE));
                ramPatterns.add(Pattern.compile(TOT + "b", Pattern.CASE_INSENSITIVE));

                ramPatterns.add(Pattern.compile("%n", Pattern.CASE_INSENSITIVE));
            }

            String copy = ramFormat;

            double av = Tuils.freeRam(activityManager, memory);
            double tot = Tuils.totalRam() * 1024L;

            copy = ramPatterns.get(0).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) av, Tuils.TERA)));
            copy = ramPatterns.get(1).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) av, Tuils.GIGA)));
            copy = ramPatterns.get(2).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) av, Tuils.MEGA)));
            copy = ramPatterns.get(3).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) av, Tuils.KILO)));
            copy = ramPatterns.get(4).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) av, Tuils.BYTE)));
            copy = ramPatterns.get(5).matcher(copy).replaceAll(String.valueOf(Tuils.percentage(av, tot)));

            copy = ramPatterns.get(6).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) tot, Tuils.TERA)));
            copy = ramPatterns.get(7).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) tot, Tuils.GIGA)));
            copy = ramPatterns.get(8).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) tot, Tuils.MEGA)));
            copy = ramPatterns.get(9).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) tot, Tuils.KILO)));
            copy = ramPatterns.get(10).matcher(copy).replaceAll(String.valueOf(Tuils.formatSize((long) tot, Tuils.BYTE)));

            copy = ramPatterns.get(11).matcher(copy).replaceAll(Tuils.NEWLINE);

            ram.setText(copy);
            ram.postDelayed(this, RAM_DELAY);
        }
    };

    private boolean showSuggestions;
    private LinearLayout suggestionsView;
    private SuggestionViewDecorer suggestionViewDecorer;
    private SuggestionRunnable suggestionRunnable;
    private LinearLayout.LayoutParams suggestionViewParams;
    private SuggestionsManager suggestionsManager;
    private boolean navigatingWithSpace = false;

    private TextView terminalView;
    private Thread lastSuggestionThread;
    private Handler activityHandler;
    private Runnable removeAllSuggestions = new Runnable() {
        @Override
        public void run() {
            suggestionsView.removeAllViews();
        }
    };

    private String doubleTapCmd;
    private boolean lockOnDbTap;

    protected TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int st, int b, int c) {
            requestSuggestion(s.toString());
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SuggestionsManager.Suggestion suggestion = (SuggestionsManager.Suggestion) v.getTag(R.id.suggestion_id);
            boolean execOnClick = suggestion.exec;

            String text = suggestion.getText();
            String input = mTerminalAdapter.getInput();

            if(suggestion.type == SuggestionsManager.Suggestion.TYPE_PERMANENT) {
                mTerminalAdapter.setInput(input + text);
            } else {
                if(multipleCmdSeparator.length() > 0) {
                    String[] split = input.split(multipleCmdSeparator);
                    if(split.length == 0) return;
                    if(split.length == 1) mTerminalAdapter.setInput(text);
                    else {
                        split[split.length - 1] = Tuils.EMPTYSTRING;

                        String beforeInputs = Tuils.EMPTYSTRING;
                        for(int count = 0; count < split.length - 1; count++) {
                            beforeInputs = beforeInputs + split[count] + multipleCmdSeparator;
                        }

                        mTerminalAdapter.setInput(beforeInputs + text);
                    }
                } else {
                    mTerminalAdapter.setInput(text);
                }
            }

            if (execOnClick) {
                mTerminalAdapter.simulateEnter();
            } else {
                mTerminalAdapter.focusInputEnd();
            }
        }
    };

    public void requestSuggestion(final String input) {

        if (suggestionsView == null || suggestionsManager == null || !showSuggestions) {
            return;
        }

        if (suggestionViewParams == null) {
            suggestionViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            suggestionViewParams.setMargins(SkinManager.SUGGESTION_MARGIN, 0, SkinManager.SUGGESTION_MARGIN, 0);
            suggestionViewParams.gravity = Gravity.CENTER_VERTICAL;
        }

        if(suggestionRunnable == null) {
            suggestionRunnable = new SuggestionRunnable(skinManager, suggestionsView, suggestionViewParams, (HorizontalScrollView) suggestionsView.getParent());
        }

        if(activityHandler == null) {
            Field field;
            try {
                field = mContext.getClass().getSuperclass().getDeclaredField("mHandler");
                field.setAccessible(true);
                activityHandler = (Handler) field.get(mContext);
            }
            catch (Exception e) {}
        }

        if (lastSuggestionThread != null) {
            lastSuggestionThread.interrupt();
            suggestionRunnable.interrupt();
            if(activityHandler != null) {
                activityHandler.removeCallbacks(suggestionRunnable);
            }
        }

        lastSuggestionThread = new StoppableThread() {
            @Override
            public void run() {
                super.run();

                String before, lastWord;
                String lastInput;
                if(multipleCmdSeparator.length() > 0) {
                    String[] split = input.split(multipleCmdSeparator);
                    if(split.length == 0) lastInput = input;
                    else lastInput = split[split.length - 1];
                } else {
                    lastInput = input;
                }

                int lastSpace = lastInput.lastIndexOf(Tuils.SPACE);
                if(lastSpace == -1) {
                    before = Tuils.EMPTYSTRING;
                    lastWord = lastInput;
                } else {
                    before = lastInput.substring(0,lastSpace);
                    lastWord = lastInput.substring(lastSpace + 1,lastInput.length());
                }


                final SuggestionsManager.Suggestion[] suggestions;
                try {
                    suggestions = suggestionsManager.getSuggestions(info, before, lastWord);
                } catch (Exception e) {
                    Tuils.log(e);
                    Tuils.toFile(e);
                    return;
                }

                if(suggestions.length == 0) {
                    ((Activity) mContext).runOnUiThread(removeAllSuggestions);
                    return;
                }

                if (Thread.interrupted()) {
                    suggestionRunnable.interrupt();
                    return;
                }

                final TextView[] existingViews = new TextView[suggestionsView.getChildCount()];
                for (int count = 0; count < existingViews.length; count++) {
                    existingViews[count] = (TextView) suggestionsView.getChildAt(count);
                }

                if (Thread.interrupted()) {
                    suggestionRunnable.interrupt();
                    return;
                }

                int n = suggestions.length - existingViews.length;
                TextView[] toAdd = null;
                TextView[] toRecycle = null;
                if (n == 0) {
                    toRecycle = existingViews;
                    toAdd = null;
                } else if (n > 0) {
                    toRecycle = existingViews;
                    toAdd = new TextView[n];
                    for (int count = 0; count < toAdd.length; count++) {
                        toAdd[count] = suggestionViewDecorer.getSuggestionView(mContext);
                    }
                } else if (n < 0) {
                    toAdd = null;
                    toRecycle = new TextView[suggestions.length];
                    System.arraycopy(existingViews, 0, toRecycle, 0, toRecycle.length);
                }

                if (Thread.interrupted()) {
                    suggestionRunnable.interrupt();
                    return;
                }

                suggestionRunnable.setN(n);
                suggestionRunnable.setSuggestions(suggestions);
                suggestionRunnable.setToAdd(toAdd);
                suggestionRunnable.setToRecycle(toRecycle);
                suggestionRunnable.reset();
                ((Activity) mContext).runOnUiThread(suggestionRunnable);
            }
        };

        lastSuggestionThread.start();
    }

    protected UIManager(ExecutePack info, final Context context, final ViewGroup rootView, final CommandExecuter tri, MainPack mainPack, boolean canApplyTheme) {

        policy = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        component = new ComponentName(context, PolicyReceiver.class);

        multipleCmdSeparator = XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.multiple_cmd_separator);
//        selectFirstSuggestionEnter = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.enter_first_suggestion);

        mContext = context;
        this.info = (MainPack) info;

        trigger = tri;

        final Typeface lucidaConsole = Typeface.createFromAsset(context.getAssets(), "lucida_console.ttf");

        imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        skinManager = new SkinManager();

        this.info.skinManager = skinManager;

        if (!skinManager.useSystemWp || !canApplyTheme) {
            rootView.setBackgroundColor(skinManager.bgColor);
        } else {
            rootView.setBackgroundColor(skinManager.overlayColor);
        }

//        scrolllllll
        if(XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.auto_scroll)) {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
                    if (heightDiff > Tuils.dpToPx(context, 200)) { // if more than 200 dp, it's probably a keyboard...
                        if(mTerminalAdapter != null) mTerminalAdapter.scrollToEnd();
                    }
                }
            });
        }

        int rightMM, leftMM, topMM, bottomMM;
        rightMM = XMLPrefsManager.get(int.class, XMLPrefsManager.Ui.right_margin_mm);
        leftMM = XMLPrefsManager.get(int.class, XMLPrefsManager.Ui.left_margin_mm);
        topMM = XMLPrefsManager.get(int.class, XMLPrefsManager.Ui.top_margin_mm);
        bottomMM = XMLPrefsManager.get(int.class, XMLPrefsManager.Ui.bottom_margin_mm);

        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        rootView.setPadding(Tuils.mmToPx(metrics, leftMM), Tuils.mmToPx(metrics, topMM), Tuils.mmToPx(metrics, rightMM), Tuils.mmToPx(metrics, bottomMM));

        TextView[] ts = {
                (TextView) rootView.findViewById(R.id.tv0),
                (TextView) rootView.findViewById(R.id.tv1),
                (TextView) rootView.findViewById(R.id.tv2),
                (TextView) rootView.findViewById(R.id.tv3),
                (TextView) rootView.findViewById(R.id.tv4)
        };

        int ramIndex = XMLPrefsManager.get(int.class, XMLPrefsManager.Ui.ram_index);
        int deviceIndex = XMLPrefsManager.get(int.class, XMLPrefsManager.Ui.device_index);
        int batteryIndex = XMLPrefsManager.get(int.class, XMLPrefsManager.Ui.battery_index);
        int timeIndex = XMLPrefsManager.get(int.class, XMLPrefsManager.Ui.time_index);
        int storageIndex = XMLPrefsManager.get(int.class, XMLPrefsManager.Ui.storage_index);

        final int RAM = 10, DEVICE = 11, TIME = 12, BATTERY = 13, STORAGE = 14;
        Sequence s = new Sequence(new int[] {ramIndex, deviceIndex, batteryIndex, timeIndex, storageIndex}, new Integer[] {RAM, DEVICE, BATTERY, TIME, STORAGE});

        for(int count = 0; count < s.size(); count++) {
            int i = (int) s.get(count);

            switch (i) {
                case RAM:
                    ram = ts[count];
                    break;
                case DEVICE:
                    device = ts[count];
                    break;
                case BATTERY:
                    battery = ts[count];
                    break;
                case STORAGE:
                    storage = ts[count];
                    break;
                case TIME:
                    time = ts[count];
                    break;
            }
        }

        boolean showRam = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_ram);
        if (showRam) {
            ram.setTextColor(skinManager.ramColor);
            ram.setTextSize(skinManager.getTextSize());
            ram.setTypeface(skinManager.systemFont ? Typeface.DEFAULT : lucidaConsole);

            memory = new ActivityManager.MemoryInfo();
            activityManager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);

            ram.post(ramRunnable);
        } else {
            ram.setVisibility(View.GONE);
            ram = null;
        }

        boolean showStorage = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_storage_info);
        if(showStorage) {
            storage.setTextColor(skinManager.storageColor);
            storage.setTextSize(skinManager.getTextSize());
            storage.setTypeface(skinManager.systemFont ? Typeface.DEFAULT : lucidaConsole);

            storage.post(storageRunnable);
        } else {
            storage.setVisibility(View.GONE);
        }

        boolean showDevice = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_device_name);
        if (showDevice) {

            Pattern USERNAME = Pattern.compile("%u", Pattern.CASE_INSENSITIVE);
            Pattern DV = Pattern.compile("%d", Pattern.CASE_INSENSITIVE);
            Pattern NEWLINE = Pattern.compile("%n", Pattern.CASE_INSENSITIVE);

            String deviceFormat = XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.device_format);

            deviceFormat = USERNAME.matcher(deviceFormat).replaceAll(skinManager.username != null ? skinManager.username : "null");
            deviceFormat = DV.matcher(deviceFormat).replaceAll(skinManager.deviceName != null ? skinManager.deviceName : "null");
            deviceFormat = NEWLINE.matcher(deviceFormat).replaceAll(Tuils.NEWLINE);

            device.setText(deviceFormat);
            device.setTextColor(skinManager.deviceColor);
            device.setTextSize(skinManager.getTextSize());
            device.setTypeface(skinManager.systemFont ? Typeface.DEFAULT : lucidaConsole);
        } else {
            device.setVisibility(View.GONE);
        }

        boolean showTime = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_time);
        if(showTime) {
            time.setTextColor(skinManager.time_color);
            time.setTextSize(skinManager.getTextSize());
            time.setTypeface(skinManager.systemFont ? Typeface.DEFAULT : lucidaConsole);

            time.post(timeRunnable);
        } else {
            time.setVisibility(View.GONE);
        }

        boolean showBattery = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_battery);
        if(showBattery) {
            mediumPercentage = XMLPrefsManager.get(int.class, XMLPrefsManager.Behavior.battery_medium);
            lowPercentage = XMLPrefsManager.get(int.class, XMLPrefsManager.Behavior.battery_low);

            if(mediumPercentage < lowPercentage) skinManager.manyColorsBattery = false;

            battery.setTextSize(skinManager.getTextSize());
            battery.setTypeface(skinManager.systemFont ? Typeface.DEFAULT : lucidaConsole);

            Tuils.registerBatteryReceiver(context, batteryUpdate);
        } else {
            battery.setVisibility(View.GONE);
        }

        final boolean inputBottom = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.input_bottom);
        int layoutId = inputBottom ? R.layout.input_down_layout : R.layout.input_up_layout;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View inputOutputView = inflater.inflate(layoutId, null);
        rootView.addView(inputOutputView);

        terminalView = (TextView) inputOutputView.findViewById(R.id.terminal_view);
        terminalView.setOnTouchListener(this);
        ((View) terminalView.getParent().getParent()).setOnTouchListener(this);

        final EditText inputView = (EditText) inputOutputView.findViewById(R.id.input_view);
        inputView.setOnTouchListener(this);

        TextView prefixView = (TextView) inputOutputView.findViewById(R.id.prefix_view);

        ImageButton submitView = (ImageButton) inputOutputView.findViewById(R.id.submit_tv);
        boolean showSubmit = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_enter_button);
        if (!showSubmit) {
            submitView.setVisibility(View.GONE);
            submitView = null;
        }

//        toolbar
        boolean showToolbar = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Toolbar.show_toolbar);
        ImageButton backView = null;
        ImageButton nextView = null;
        ImageButton deleteView = null;
        ImageButton pasteView = null;

        if(!showToolbar) {
            inputOutputView.findViewById(R.id.tools_view).setVisibility(View.GONE);
        } else {
            backView = (ImageButton) inputOutputView.findViewById(R.id.back_view);
            nextView = (ImageButton) inputOutputView.findViewById(R.id.next_view);
            deleteView = (ImageButton) inputOutputView.findViewById(R.id.delete_view);
            pasteView = (ImageButton) inputOutputView.findViewById(R.id.paste_view);
        }

        if (skinManager.showSuggestions) {
            showSuggestions = true;

            HorizontalScrollView sv = (HorizontalScrollView) rootView.findViewById(R.id.suggestions_container);
            sv.setFocusable(false);
            sv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(hasFocus) {
                        v.clearFocus();
                    }
                }
            });

            suggestionsView = (LinearLayout) rootView.findViewById(R.id.suggestions_group);

            inputView.addTextChangedListener(textWatcher);

            suggestionsManager = new SuggestionsManager();

            this.suggestionViewDecorer = new SuggestionViewDecorer() {
                @Override
                public TextView getSuggestionView(Context context) {
                    TextView textView = new TextView(mContext);
                    textView.setOnClickListener(clickListener);

                    textView.setFocusable(false);
                    textView.setLongClickable(false);
                    textView.setClickable(true);

                    textView.setTypeface(skinManager.systemFont ? Typeface.DEFAULT : lucidaConsole);
                    textView.setTextSize(skinManager.getSuggestionSize());

                    textView.setPadding(SkinManager.SUGGESTION_PADDING_HORIZONTAL, SkinManager.SUGGESTION_PADDING_VERTICAL,
                            SkinManager.SUGGESTION_PADDING_HORIZONTAL, SkinManager.SUGGESTION_PADDING_VERTICAL);

                    textView.setLines(1);
                    textView.setMaxLines(1);
                    return textView;
                }
            };
        } else {
            showSuggestions = false;
            rootView.findViewById(R.id.suggestions_group).setVisibility(View.GONE);
            this.textWatcher = null;
            this.clickListener = null;
        }

        lockOnDbTap = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.double_tap_closes);
        doubleTapCmd = XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.double_tap_cmd);
        if(!lockOnDbTap && doubleTapCmd == null) {
            policy = null;
            component = null;
            det = null;
        } else initDetector();

        mTerminalAdapter = new TerminalManager(terminalView, inputView, prefixView, submitView, backView, nextView, deleteView, pasteView, skinManager, context, mainPack);
        mTerminalAdapter.setInputListener(inputListener);
        if(XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.donation_message)) {
            mTerminalAdapter.addMessager(new TerminalManager.Messager(20, context.getString(R.string.rate_donate_text)));
        }
    }

    public void openKeyboard() {
        mTerminalAdapter.requestInputFocus();
        imm.showSoftInput(mTerminalAdapter.getInputView(), InputMethodManager.SHOW_IMPLICIT);
        mTerminalAdapter.scrollToEnd();
    }

    public void closeKeyboard() {
        imm.hideSoftInputFromWindow(mTerminalAdapter.getInputWindowToken(), 0);
    }

    public void onStart(boolean openKeyboardOnStart) {
        if(openKeyboardOnStart) openKeyboard();
    }

    public void setInput(String s) {
        if (s == null)
            return;

        mTerminalAdapter.setInput(s);
        mTerminalAdapter.focusInputEnd();
    }

    public void setHint(String hint) {
        mTerminalAdapter.setHint(hint);
    }

    public void resetHint() {
        mTerminalAdapter.setDefaultHint();
    }

    public void setOutput(CharSequence s, int category) {
        mTerminalAdapter.setOutput(s, category);
    }

    public void disableSuggestions() {
        if(suggestionsView != null) {
            showSuggestions = false;
            suggestionsView.setVisibility(View.GONE);
        }
    }

    public void enableSuggestions() {
        if(suggestionsView != null) {
            showSuggestions = true;
            suggestionsView.setVisibility(View.VISIBLE);
        }
    }

    public void onBackPressed() {
        mTerminalAdapter.onBackPressed();
    }

    public void focusTerminal() {
        mTerminalAdapter.requestInputFocus();
    }

    public void scrollToEnd() {
        mTerminalAdapter.scrollToEnd();
    }

    public Hintable getHintable() {
        return new Hintable() {
            @Override
            public void updateHint() {
                mTerminalAdapter.setDefaultHint();
            }
        };
    }

    public Rooter getRooter() {
        return mTerminalAdapter.getRooter();
    }

    //	 init detector for double tap
    private void initDetector() {
        det = new GestureDetector(mContext, TrashInterfaces.trashGestureListener);

        det.setOnDoubleTapListener(new OnDoubleTapListener() {

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if(doubleTapCmd != null && doubleTapCmd.length() > 0) {
                    String input = mTerminalAdapter.getInput();
                    mTerminalAdapter.setInput(doubleTapCmd);
                    mTerminalAdapter.simulateEnter();
                    mTerminalAdapter.setInput(input);
                }

                if(lockOnDbTap) {
                    boolean admin = policy.isAdminActive(component);

                    if (!admin) {
                        Intent i = Tuils.requestAdmin(component);
                        mContext.startActivity(i);
                    } else {
                        policy.lockNow();
                    }
                }

                return true;
            }
        });
    }

    protected boolean verifyDoubleTap(MotionEvent event) {
        boolean b = det.onTouchEvent(event);
        return det != null && b;
    }

    //	 on pause
    public void pause() {
        closeKeyboard();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (verifyDoubleTap(event)) {
            return true;
        }

        if (event.getAction() != MotionEvent.ACTION_DOWN)
            return v.onTouchEvent(event);

        if (v.getId() == R.id.input_view) {
            openKeyboard();
            return true;
        } else
            return v.onTouchEvent(event);
    }

    public OnRedirectionListener buildRedirectionListener() {
        return new OnRedirectionListener() {
            @Override
            public void onRedirectionRequest(final RedirectCommand cmd) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTerminalAdapter.setHint(mContext.getString(cmd.getHint()));
                        disableSuggestions();
                    }
                });
            }

            @Override
            public void onRedirectionEnd(RedirectCommand cmd) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTerminalAdapter.setDefaultHint();
                        enableSuggestions();
                    }
                });
            }
        };
    }

    public interface SuggestionNavigator {
        boolean isNavigating();
        void onEnter();
    }

    public interface OnNewInputListener {
        void onNewInput(String input);
    }

//    public class PowerConnectionReceiver extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
//            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
//
//            if(isCharging) {
//                battery.postDelayed(batteryChargingRunnable, BATTERY_CHARGING_DELAY);
//            } else {
//                battery.removeCallbacks(batteryChargingRunnable);
//            }
//        }
//    }

}

