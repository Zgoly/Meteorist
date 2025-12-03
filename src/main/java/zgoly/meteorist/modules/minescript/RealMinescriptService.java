package zgoly.meteorist.modules.minescript;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import net.minescript.common.JobControl;
import net.minescript.common.JobState;
import net.minescript.common.Minescript;
import zgoly.meteorist.mixin.MinescriptAccessor;
import zgoly.meteorist.utils.MeteoristUtils;
import zgoly.meteorist.utils.misc.DebugLogger;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class RealMinescriptService implements MinescriptService {
    private final Setting<Boolean> censorJWord;
    private final DebugLogger debugLogger;

    private final List<String> scriptStates = new ArrayList<>();
    private final Map<Integer, Integer> jobTickCounts = new HashMap<>();
    private GuiTheme currentTheme;
    private WVerticalList rootList;
    private final List<WLabel> currentJobLabels = new ArrayList<>();
    private boolean isEnabled = false;
    private Set<Map.Entry<Integer, JobState>> lastJobStateSnapshot = Collections.emptySet();

    public static Object jobs;

    public RealMinescriptService(MinescriptIntegration module) {
        SettingGroup sgGeneral = module.settings.getDefaultGroup();

        censorJWord = sgGeneral.add(new BoolSetting.Builder()
                .name("censor-j-word")
                .description("Censors the J-word as 'J*b'.")
                .defaultValue(true)
                .onChanged(v -> onCensorSettingChanged())
                .build()
        );

        debugLogger = new DebugLogger(module, module.settings);
    }

    public void onCensorSettingChanged() {
        if (currentTheme != null && rootList != null) {
            refreshWidget();
        }
    }

    @Override
    public WWidget createWidget(GuiTheme theme) {
        currentTheme = theme;
        rootList = theme.verticalList();
        refreshWidget();
        return rootList;
    }

    private void refreshWidget() {
        if (currentTheme == null || rootList == null) return;

        rootList.clear();
        currentJobLabels.clear();
        refreshScripts();

        var table = rootList.add(currentTheme.table()).expandX().widget();

        if (scriptStates.isEmpty()) {
            table.add(currentTheme.label("No custom scripts found in minescript/")).expandX();
            return;
        }

        table.add(currentTheme.horizontalSeparator("Custom scripts")).expandX();
        table.row();

        for (String script : scriptStates) {
            table.add(currentTheme.label(script)).expandX();
            WButton btn = table.add(currentTheme.button("Run")).widget();
            btn.action = () -> MinescriptAccessor.runMinescriptCommand(script);
            table.row();
        }

        var jobsTable = table.add(currentTheme.table()).expandX().widget();
        Map<Integer, JobControl> activeJobs = getJobMap();

        lastJobStateSnapshot = activeJobs.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().state()))
                .collect(Collectors.toSet());

        if (!isEnabled) {
            jobsTable.add(currentTheme.horizontalSeparator()).expandX();
            jobsTable.row();
            jobsTable.add(currentTheme.label(censored("Module inactive. Activate the module in-world to track job activity."))).center().alignWidget();
        } else {
            if (!activeJobs.isEmpty()) {
                jobsTable.add(currentTheme.horizontalSeparator(censored("Jobs"))).expandX();
                jobsTable.row();
            }

            activeJobs.forEach((id, job) -> {
                if (job.state() == JobState.KILLED) {
                    jobTickCounts.remove(id);
                    debugLogger.info(censored("Removed killed job ID: (highlight)%d(default)"), id);
                    return;
                }

                jobsTable.add(currentTheme.label(job.jobSummary())).expandX();
                currentJobLabels.add(jobsTable.add(currentTheme.label("")).expandX().widget());

                WButton suspendBtn = jobsTable.add(currentTheme.button(
                        job.state() == JobState.SUSPENDED ? "Resume" : "Suspend"
                )).expandX().widget();

                suspendBtn.action = () -> {
                    if (job.state() == JobState.SUSPENDED) {
                        job.resume();
                        suspendBtn.set("Resume");
                    } else {
                        job.suspend();
                        suspendBtn.set("Suspend");
                    }
                    debugLogger.info(censored("%s job ID: (highlight)%d(default)"),
                            job.state() == JobState.SUSPENDED ? "Resumed" : "Suspended", id);
                    refreshWidget();
                };

                WButton stopBtn = jobsTable.add(currentTheme.button("Stop")).expandX().widget();
                stopBtn.action = () -> {
                    job.requestKill();
                    jobTickCounts.remove(id);
                    debugLogger.info(censored("Requested kill for job ID: (highlight)%d(default)"), id);
                    refreshWidget();
                };

                jobsTable.row();
            });
        }
    }

    public void refreshScripts() {
        scriptStates.clear();
        List<String> scripts = Minescript.config.scriptConfig().findCommandPrefixMatches("");
        for (String script : scripts) {
            if (!MinescriptAccessor.getBuiltinCommands().contains(script)) {
                scriptStates.add(script);
            }
        }
    }

    @Override
    public void refreshJobsIfNeeded() {
        Map<Integer, JobControl> currentJobs = getJobMap();
        Set<Map.Entry<Integer, JobState>> currentSnapshot = currentJobs.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().state()))
                .collect(Collectors.toSet());

        if (!currentSnapshot.equals(lastJobStateSnapshot)) {
            for (Integer id : currentJobs.keySet()) jobTickCounts.putIfAbsent(id, 0);
            jobTickCounts.keySet().removeIf(id -> !currentJobs.containsKey(id));
            refreshWidget();
        }
    }

    @Override
    public void onTick() {
        refreshJobsIfNeeded();
        Map<Integer, JobControl> activeJobs = getJobMap();
        int index = 0;
        for (var entry : activeJobs.entrySet()) {
            Integer id = entry.getKey();
            JobControl job = entry.getValue();

            if (job.state() != JobState.SUSPENDED && job.state() != JobState.KILLED) {
                jobTickCounts.merge(id, 1, Integer::sum);
            }

            if (index < currentJobLabels.size()) {
                int ticks = jobTickCounts.getOrDefault(id, 0);
                String timeStr = MeteoristUtils.ticksToTime(ticks, true, false);
                currentJobLabels.get(index).set(timeStr);
            }
            index++;
        }
    }

    private String censored(String input) {
        if (!censorJWord.get()) return input;
        return input.replaceAll("(?i)(j)o(b)", "$1*$2");
    }

    @Override
    public void onActivate() {
        isEnabled = true;
        refreshWidget();
    }

    @Override
    public void onDeactivate() {
        jobTickCounts.clear();
        isEnabled = false;
        refreshWidget();
    }

    @SuppressWarnings("unchecked")
    public static Map<Integer, JobControl> getJobMap() {
        try {
            Field jobManagerField = Minescript.class.getDeclaredField("jobs");
            jobManagerField.setAccessible(true);
            Object jobManager = jobManagerField.get(null);

            Field jobMapField = jobManager.getClass().getDeclaredField("jobMap");
            jobMapField.setAccessible(true);

            return (Map<Integer, JobControl>) jobMapField.get(jobManager);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
