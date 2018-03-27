package de.symeda.sormas.app.event.read;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;

import de.symeda.sormas.api.event.EventStatus;
import de.symeda.sormas.app.BaseReadActivity;
import de.symeda.sormas.app.BaseReadActivityFragment;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.backend.event.Event;
import de.symeda.sormas.app.component.menu.LandingPageMenuItem;
import de.symeda.sormas.app.core.BoolResult;
import de.symeda.sormas.app.core.async.IJobDefinition;
import de.symeda.sormas.app.core.async.ITaskExecutor;
import de.symeda.sormas.app.core.async.ITaskResultCallback;
import de.symeda.sormas.app.core.async.ITaskResultHolderIterator;
import de.symeda.sormas.app.core.async.TaskExecutorFor;
import de.symeda.sormas.app.core.async.TaskResultHolder;
import de.symeda.sormas.app.shared.EventFormNavigationCapsule;
import de.symeda.sormas.app.event.edit.EventEditActivity;
import de.symeda.sormas.app.util.ConstantHelper;
import de.symeda.sormas.app.util.NavigationHelper;

/**
 * Created by Orson on 24/12/2017.
 */

public class EventReadActivity extends BaseReadActivity {

    private final String DATA_XML_PAGE_MENU = "xml/data_read_page_alert_menu.xml";

    private static final int MENU_INDEX_EVENT_INFO = 0;
    private static final int MENU_INDEX_EVENT__PERSON_INVOLVED = 1;
    private static final int MENU_INDEX_EVENT_TASK = 2;

    private AsyncTask jobTask;
    private EventStatus filterStatus = null;
    private EventStatus pageStatus = null;
    private String recordUuid = null;
    private LandingPageMenuItem activeMenuItem = null;
    private int activeMenuKey = ConstantHelper.INDEX_FIRST_MENU;
    private BaseReadActivityFragment activeFragment = null;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        SaveFilterStatusState(outState, filterStatus);
        SavePageStatusState(outState, pageStatus);
        SaveRecordUuidState(outState, recordUuid);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*if (savedInstanceState == null) {
            Bundle b = getIntent().getExtras();
            if (b != null){
                //Get Shipment Status
                filterStatus = getEventStatusArg(b);

                //Get Event Key
                recordUuid = getEventUuidArg(b);
            }

            activeMenuKey = ConstantHelper.INDEX_FIRST_MENU;
            filterStatus = (filterStatus == null)? EventStatus.POSSIBLE : filterStatus;
        } else {
            filterStatus = (EventStatus) savedInstanceState.get(ConstantHelper.ARG_FILTER_STATUS);
            recordUuid = savedInstanceState.getString(ConstantHelper.KEY_EVENT_UUID);
            activeMenuKey = savedInstanceState.getInt(ConstantHelper.KEY_ACTIVE_MENU);
        }*/
    }

    @Override
    protected void initializeActivity(Bundle arguments) {
        filterStatus = (EventStatus) getFilterStatusArg(arguments);
        pageStatus = (EventStatus) getPageStatusArg(arguments);
        recordUuid = getRecordUuidArg(arguments);
    }

    @Override
    public BaseReadActivityFragment getActiveReadFragment() throws IllegalAccessException, InstantiationException {
        if (activeFragment == null) {
            EventFormNavigationCapsule dataCapsule = new EventFormNavigationCapsule(EventReadActivity.this,
                    recordUuid, pageStatus);
            activeFragment = EventReadFragment.newInstance(this, dataCapsule);
        }

        return activeFragment;
    }

    @Override
    public LandingPageMenuItem getActiveMenuItem() {
        return activeMenuItem;
    }

    @Override
    public boolean showStatusFrame() {
        return true;
    }

    @Override
    public boolean showTitleBar() {
        return true;
    }

    @Override
    public boolean showPageMenu() {
        return true;
    }

    @Override
    public Enum getPageStatus() {
        return pageStatus;
    }

    @Override
    public String getPageMenuData() {
        return DATA_XML_PAGE_MENU;
    }

    @Override
    public boolean onLandingPageMenuClick(AdapterView<?> parent, View view, LandingPageMenuItem menuItem, int position, long id) throws IllegalAccessException, InstantiationException {
        setActiveMenu(menuItem);

        EventFormNavigationCapsule dataCapsule = new EventFormNavigationCapsule(EventReadActivity.this,
                recordUuid, pageStatus);

        if (menuItem.getKey() == MENU_INDEX_EVENT_INFO) {
            activeFragment = EventReadFragment.newInstance(this, dataCapsule);
        } else if (menuItem.getKey() == MENU_INDEX_EVENT__PERSON_INVOLVED) {
            activeFragment = EventReadPersonsInvolvedFragment.newInstance(this, dataCapsule);
        } else if (menuItem.getKey() == MENU_INDEX_EVENT_TASK) {
            activeFragment = EventReadTaskListFragement.newInstance(this, dataCapsule);
        }

        replaceFragment(activeFragment);
        updateSubHeadingTitle();

        return true;
    }

    @Override
    public LandingPageMenuItem onSelectInitialActiveMenuItem(ArrayList<LandingPageMenuItem> menuList) {
        activeMenuItem = menuList.get(0);

        for(LandingPageMenuItem m: menuList){
            if (m.getKey() == activeMenuKey){
                activeMenuItem = m;
            }
        }

        return activeMenuItem;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.read_action_menu, menu);

        MenuItem readMenu = menu.findItem(R.id.action_edit);
        //readMenu.setVisible(false);
        readMenu.setTitle(R.string.action_edit_event);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavigationHelper.navigateUpFrom(this);
                return true;

            case R.id.action_edit:
                gotoEditView();
                return true;

            case R.id.option_menu_action_sync:
                //synchronizeChangedData();
                return true;

            case R.id.option_menu_action_markAllAsRead:
                /*CaseDao caseDao = DatabaseHelper.getCaseDao();
                PersonDao personDao = DatabaseHelper.getPersonDao();
                List<Case> cases = caseDao.queryForAll();
                for (Case caseToMark : cases) {
                    caseDao.markAsRead(caseToMark);
                }

                for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                    if (fragment instanceof CasesListFragment) {
                        fragment.onResume();
                    }
                }*/
                return true;

            // Report problem button
            case R.id.action_report:
                /*UserReportDialog userReportDialog = new UserReportDialog(this, this.getClass().getSimpleName(), null);
                AlertDialog dialog = userReportDialog.create();
                dialog.show();*/

                return true;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getActivityTitle() {
        return R.string.heading_level3_event_read;
    }


    private void gotoEditView() {
        if (activeFragment == null)
            return;

        try {
            ITaskExecutor executor = TaskExecutorFor.job(new IJobDefinition() {
                @Override
                public void preExecute(BoolResult resultStatus, TaskResultHolder resultHolder) {
                    //showPreloader();
                    //hideFragmentView();
                }

                @Override
                public void execute(BoolResult resultStatus, TaskResultHolder resultHolder) {
                    Event record = DatabaseHelper.getEventDao().queryUuid(recordUuid);

                    if (record == null) {
                        // build a new event for empty uuid
                        resultHolder.forItem().add(DatabaseHelper.getEventDao().build());
                    } else {
                        resultHolder.forItem().add(record);
                    }
                }
            });
            jobTask = executor.execute(new ITaskResultCallback() {
                @Override
                public void taskResult(BoolResult resultStatus, TaskResultHolder resultHolder) {
                    //hidePreloader();
                    //showFragmentView();

                    if (resultHolder == null)
                        return;

                    ITaskResultHolderIterator itemIterator = resultHolder.forItem().iterator();
                    if (itemIterator.hasNext()) {
                        Event record = itemIterator.next();

                        EventFormNavigationCapsule dataCapsule = new EventFormNavigationCapsule(EventReadActivity.this,
                                record.getUuid(), pageStatus);
                        EventEditActivity.goToActivity(EventReadActivity.this, dataCapsule);
                    }
                }
            });
        } catch (Exception ex) {
            //hidePreloader();
            //showFragmentView();
        }
    }

    public static void goToActivity(Context fromActivity, EventFormNavigationCapsule dataCapsule) {
        BaseReadActivity.goToActivity(fromActivity, EventReadActivity.class, dataCapsule);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (jobTask != null && !jobTask.isCancelled())
            jobTask.cancel(true);
    }
}

