package de.symeda.sormas.app.person.edit;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.I18nProperties;
import de.symeda.sormas.api.person.ApproximateAgeType;
import de.symeda.sormas.api.person.BurialConductor;
import de.symeda.sormas.api.person.CauseOfDeath;
import de.symeda.sormas.api.person.DeathPlaceType;
import de.symeda.sormas.api.person.OccupationType;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.person.PresentCondition;
import de.symeda.sormas.api.person.Sex;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.app.BaseActivity;
import de.symeda.sormas.app.BaseEditFragment;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.caze.Case;
import de.symeda.sormas.app.backend.common.AbstractDomainObject;
import de.symeda.sormas.app.backend.contact.Contact;
import de.symeda.sormas.app.backend.location.Location;
import de.symeda.sormas.app.backend.person.Person;
import de.symeda.sormas.app.component.Item;
import de.symeda.sormas.app.component.controls.ControlPropertyField;
import de.symeda.sormas.app.component.controls.ValueChangeListener;
import de.symeda.sormas.app.component.dialog.LocationDialog;
import de.symeda.sormas.app.component.dialog.TeboAlertDialogInterface;
import de.symeda.sormas.app.core.BaseFormNavigationCapsule;
import de.symeda.sormas.app.databinding.FragmentPersonEditLayoutBinding;
import de.symeda.sormas.app.util.DataUtils;
import de.symeda.sormas.app.util.InfrastructureHelper;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class PersonEditFragment extends BaseEditFragment<FragmentPersonEditLayoutBinding, Person, AbstractDomainObject> {

    public static final String TAG = PersonEditFragment.class.getSimpleName();

    private Person record;

    // Instance methods

    public static PersonEditFragment newInstance(BaseFormNavigationCapsule capsule, AbstractDomainObject activityRootData) {
        return newInstance(PersonEditFragment.class, capsule, activityRootData);
    }

    public static void setUpLayoutBinding(final BaseEditFragment fragment, final Person record, final FragmentPersonEditLayoutBinding contentBinding) {
        setUpControlListeners(record, contentBinding);

        List<Item> dayList = DataUtils.toItems(DateHelper.getDaysInMonth(), true);
        List<Item> monthList = DataUtils.getMonthItems(true);
        List<Item> yearList = DataUtils.toItems(DateHelper.getYearsToNow(), true);
        List<Item> approximateAgeTypeList = DataUtils.getEnumItems(ApproximateAgeType.class, true);
        List<Item> sexList = DataUtils.getEnumItems(Sex.class, true);
        List<Item> causeOfDeathList = DataUtils.getEnumItems(CauseOfDeath.class, true);
        List<Item> diseaseList = DataUtils.getEnumItems(Disease.class, true);
        List<Item> deathPlaceTypeList = DataUtils.getEnumItems(DeathPlaceType.class, true);
        List<Item> burialConductorList = DataUtils.getEnumItems(BurialConductor.class, true);
        List<Item> occupationTypeList = DataUtils.getEnumItems(OccupationType.class, true);

        List<Item> initialOccupationRegions = InfrastructureHelper.loadRegions();
        List<Item> initialOccupationDistricts = InfrastructureHelper.loadDistricts(record.getOccupationRegion());
        List<Item> initialOccupationCommunities = InfrastructureHelper.loadCommunities(record.getOccupationDistrict());
        List<Item> initialOccupationFacilities = InfrastructureHelper.loadFacilities(record.getOccupationDistrict(), record.getOccupationCommunity());

        InfrastructureHelper.initializeHealthFacilityDetailsFieldVisibility(contentBinding.personOccupationFacility, contentBinding.personOccupationFacilityDetails);
        initializeCauseOfDeathDetailsFieldVisibility(contentBinding.personCauseOfDeath, contentBinding.personCauseOfDeathDisease, contentBinding.personCauseOfDeathDetails);
        initializeOccupationDetailsFieldVisibility(contentBinding.personOccupationType, contentBinding.personOccupationDetails);

        InfrastructureHelper.initializeFacilityFields(contentBinding.personOccupationRegion, initialOccupationRegions,
                contentBinding.personOccupationDistrict, initialOccupationDistricts,
                contentBinding.personOccupationCommunity, initialOccupationCommunities,
                contentBinding.personOccupationFacility, initialOccupationFacilities);

        // Initialize ControlSpinnerFields
        contentBinding.personBirthdateDD.initializeSpinner(dayList, new ValueChangeListener() {
            @Override
            public void onChange(ControlPropertyField field) {
                updateApproximateAgeField(contentBinding);
            }
        });
        contentBinding.personBirthdateMM.initializeSpinner(monthList, new ValueChangeListener() {
            @Override
            public void onChange(ControlPropertyField field) {
                updateApproximateAgeField(contentBinding);
            }
        });
        contentBinding.personBirthdateYYYY.initializeSpinner(yearList, new ValueChangeListener() {
            @Override
            public void onChange(ControlPropertyField field) {
                updateApproximateAgeField(contentBinding);
            }
        });
        contentBinding.personBirthdateYYYY.setSelectionOnOpen(2000);
        contentBinding.personApproximateAgeType.initializeSpinner(approximateAgeTypeList);
        contentBinding.personSex.initializeSpinner(sexList);
        contentBinding.personCauseOfDeath.initializeSpinner(causeOfDeathList);
        contentBinding.personCauseOfDeathDisease.initializeSpinner(diseaseList);
        contentBinding.personDeathPlaceType.initializeSpinner(deathPlaceTypeList);
        contentBinding.personBurialConductor.initializeSpinner(burialConductorList);
        contentBinding.personOccupationType.initializeSpinner(occupationTypeList);

        // Initialize ControlDateFields
        contentBinding.personDeathDate.initializeDateField(fragment.getFragmentManager());
        contentBinding.personBurialDate.initializeDateField(fragment.getFragmentManager());
    }

    public static void setUpControlListeners(final Person record, final FragmentPersonEditLayoutBinding contentBinding) {
        contentBinding.personAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAddressPopup(record, contentBinding);
            }
        });
    }

    private static void updateApproximateAgeField(FragmentPersonEditLayoutBinding contentBinding) {
        Integer birthYear = (Integer) contentBinding.personBirthdateYYYY.getValue();

        if (birthYear != null) {
            contentBinding.personApproximateAge.setEnabled(false);
            contentBinding.personApproximateAgeType.setEnabled(false);

            Integer birthDay = (Integer) contentBinding.personBirthdateDD.getValue();
            Integer birthMonth = (Integer) contentBinding.personBirthdateMM.getValue();

            Calendar birthDate = new GregorianCalendar();
            birthDate.set(birthYear, birthMonth != null ? birthMonth - 1 : 0, birthDay != null ? birthDay : 1);

            Date to = new Date();
            if (contentBinding.personDeathDate != null) {
                to = contentBinding.personDeathDate.getValue();
            }

            DataHelper.Pair<Integer, ApproximateAgeType> approximateAge = ApproximateAgeType.ApproximateAgeHelper.getApproximateAge(birthDate.getTime(), to);
            ApproximateAgeType ageType = approximateAge.getElement1();
            contentBinding.personApproximateAge.setValue(String.valueOf(approximateAge.getElement0()));
            contentBinding.personApproximateAgeType.setValue(ageType);
        } else {
            contentBinding.personApproximateAge.setEnabled(true);
            contentBinding.personApproximateAgeType.setEnabled(true);
        }
    }

    private static void openAddressPopup(final Person record, final FragmentPersonEditLayoutBinding contentBinding) {
        final Location location = record.getAddress();
        final LocationDialog locationDialog = new LocationDialog(BaseActivity.getActiveActivity(), location);
        locationDialog.show(null);

        locationDialog.setOnPositiveClickListener(new TeboAlertDialogInterface.PositiveOnClickListener() {
            @Override
            public void onOkClick(View v, Object item, View viewRoot) {
                contentBinding.personAddress.setValue(location);
                locationDialog.dismiss();
            }
        });
    }

    /**
     * Only show the causeOfDeathDetails field when either the selected cause of death is 'Other cause'
     * or the selected cause of death disease is 'Other'. Additionally, adjust the caption of the
     * causeOfDeathDetails field based on the selected options.
     */
    public static void initializeCauseOfDeathDetailsFieldVisibility(final ControlPropertyField causeOfDeathField, final ControlPropertyField causeOfDeathDiseaseField, final ControlPropertyField causeOfDeathDetailsField) {
        setCauseOfDeathDetailsFieldVisibility(causeOfDeathField, causeOfDeathDiseaseField, causeOfDeathDetailsField);
        causeOfDeathField.addValueChangedListener(new ValueChangeListener() {
            @Override
            public void onChange(ControlPropertyField field) {
                setCauseOfDeathDetailsFieldVisibility(causeOfDeathField, causeOfDeathDiseaseField, causeOfDeathDetailsField);
            }
        });
        causeOfDeathDiseaseField.addValueChangedListener(new ValueChangeListener() {
            @Override
            public void onChange(ControlPropertyField field) {
                setCauseOfDeathDetailsFieldVisibility(causeOfDeathField, causeOfDeathDiseaseField, causeOfDeathDetailsField);
            }
        });
    }

    private static void setCauseOfDeathDetailsFieldVisibility(final ControlPropertyField causeOfDeathField, final ControlPropertyField causeOfDeathDiseaseField, final ControlPropertyField causeOfDeathDetailsField) {
        CauseOfDeath selectedCauseOfDeath = (CauseOfDeath) causeOfDeathField.getValue();
        Disease selectedCauseOfDeathDisease = (Disease) causeOfDeathDiseaseField.getValue();

        if (selectedCauseOfDeath == CauseOfDeath.OTHER_CAUSE) {
            causeOfDeathDetailsField.setVisibility(VISIBLE);
            causeOfDeathDetailsField.setCaption(I18nProperties.getPrefixFieldCaption(PersonDto.I18N_PREFIX, PersonDto.CAUSE_OF_DEATH_DETAILS));
        } else if (selectedCauseOfDeathDisease == Disease.OTHER) {
            causeOfDeathDetailsField.setVisibility(VISIBLE);
            causeOfDeathDetailsField.setCaption(I18nProperties.getPrefixFieldCaption(PersonDto.I18N_PREFIX, PersonDto.CAUSE_OF_DEATH_DISEASE_DETAILS));
        } else {
            causeOfDeathDetailsField.setVisibility(GONE);
        }
    }

    /**
     * Only show the occupationDetails field when an appropriate occupation is selected. Additionally,
     * adjust the caption of the occupationDetails field based on the selected occupation.
     */
    public static void initializeOccupationDetailsFieldVisibility(final ControlPropertyField occupationTypeField, final ControlPropertyField occupationDetailsField) {
        setOccupationDetailsFieldVisibility(occupationTypeField, occupationDetailsField);
        occupationTypeField.addValueChangedListener(new ValueChangeListener() {
            @Override
            public void onChange(ControlPropertyField field) {
                setOccupationDetailsFieldVisibility(occupationTypeField, occupationDetailsField);
            }
        });
    }

    private static void setOccupationDetailsFieldVisibility(final ControlPropertyField occupationTypeField, final ControlPropertyField occupationDetailsField) {
        OccupationType selectedOccupationType = (OccupationType) occupationTypeField.getValue();
        if (selectedOccupationType != null) {
            switch (selectedOccupationType) {
                case BUSINESSMAN_WOMAN:
                    occupationDetailsField.setVisibility(VISIBLE);
                    occupationDetailsField.setCaption(I18nProperties.getFieldCaption(PersonDto.I18N_PREFIX + ".business." + PersonDto.OCCUPATION_DETAILS));
                    break;
                case TRANSPORTER:
                    occupationDetailsField.setVisibility(VISIBLE);
                    occupationDetailsField.setCaption(I18nProperties.getFieldCaption(PersonDto.I18N_PREFIX + ".transporter." + PersonDto.OCCUPATION_DETAILS));
                    break;
                case HEALTHCARE_WORKER:
                    occupationDetailsField.setVisibility(VISIBLE);
                    occupationDetailsField.setCaption(I18nProperties.getFieldCaption(PersonDto.I18N_PREFIX + ".healthcare." + PersonDto.OCCUPATION_DETAILS));
                    break;
                case OTHER:
                    occupationDetailsField.setVisibility(VISIBLE);
                    occupationDetailsField.setCaption(I18nProperties.getPrefixFieldCaption(PersonDto.I18N_PREFIX, PersonDto.OCCUPATION_DETAILS));
                    break;
                default:
                    occupationDetailsField.setVisibility(GONE);
                    break;
            }
        } else {
            occupationDetailsField.setVisibility(GONE);
        }
    }

    // Overrides

    @Override
    protected String getSubHeadingTitle() {
        return getResources().getString(R.string.caption_person_information);
    }

    @Override
    public Person getPrimaryData() {
        return record;
    }

    @Override
    protected void prepareFragmentData(Bundle savedInstanceState) {
        AbstractDomainObject ado = getActivityRootData();

        if (ado instanceof Case) {
            record = ((Case) ado).getPerson();
        } else if (ado instanceof Contact) {
            record = ((Contact) ado).getPerson();
        } else {
            throw new UnsupportedOperationException("ActivityRootData of class " + ado.getClass().getSimpleName()
                    + " does not support PersonReadFragment");
        }
    }

    @Override
    public void onLayoutBinding(FragmentPersonEditLayoutBinding contentBinding) {
        contentBinding.setData(record);
        contentBinding.setPresentConditionClass(PresentCondition.class);
    }

    @Override
    public void onAfterLayoutBinding(final FragmentPersonEditLayoutBinding contentBinding) {
        PersonEditFragment.setUpLayoutBinding(this, record, contentBinding);
    }

    @Override
    public int getEditLayout() {
        return R.layout.fragment_person_edit_layout;
    }

}
