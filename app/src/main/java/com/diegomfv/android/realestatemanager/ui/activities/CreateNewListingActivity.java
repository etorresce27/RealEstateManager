package com.diegomfv.android.realestatemanager.ui.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.diegomfv.android.realestatemanager.R;
import com.diegomfv.android.realestatemanager.RealEstateManagerApp;
import com.diegomfv.android.realestatemanager.adapters.RVAdapterMediaHorizontal;
import com.diegomfv.android.realestatemanager.constants.Constants;
import com.diegomfv.android.realestatemanager.data.entities.ImageRealEstate;
import com.diegomfv.android.realestatemanager.data.entities.RealEstate;
import com.diegomfv.android.realestatemanager.dialogfragments.InsertAddressDialogFragment;
import com.diegomfv.android.realestatemanager.network.models.placefindplacefromtext.PlaceFromText;
import com.diegomfv.android.realestatemanager.network.remote.AllGoogleServices;
import com.diegomfv.android.realestatemanager.network.remote.GoogleService;
import com.diegomfv.android.realestatemanager.network.remote.GoogleServiceStreams;
import com.diegomfv.android.realestatemanager.receivers.InternetConnectionReceiver;
import com.diegomfv.android.realestatemanager.utils.FirebasePushIdGenerator;
import com.diegomfv.android.realestatemanager.utils.ItemClickSupport;
import com.diegomfv.android.realestatemanager.utils.TextInputAutoCompleteTextView;
import com.diegomfv.android.realestatemanager.utils.ToastHelper;
import com.diegomfv.android.realestatemanager.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import retrofit2.http.Url;

/**
 * Created by Diego Fajardo on 18/08/2018.
 */

// TODO: 18/08/2018 Add a notification insertion completes!
public class CreateNewListingActivity extends AppCompatActivity implements Observer, InsertAddressDialogFragment.InsertAddressDialogListener {

    private static final String TAG = CreateNewListingActivity.class.getSimpleName();

    private RealEstateManagerApp app;

    /////////////////////////////////

    @BindView(R.id.main_layout_id)
    ScrollView mainLayout;

    @BindView(R.id.text_input_ac_text_view_type_of_bulding_id)
    TextInputAutoCompleteTextView tvTypeOfBuilding;

    @BindView(R.id.text_input_ac_text_view_price_id)
    TextInputAutoCompleteTextView tvPrice;

    @BindView(R.id.text_input_ac_text_view_surface_area_id)
    TextInputAutoCompleteTextView tvSurfaceArea;

    @BindView(R.id.text_input_ac_text_view_number_of_rooms_id)
    TextInputAutoCompleteTextView tvNumberOfRooms;

    @BindView(R.id.text_input_ac_text_view_description_id)
    TextInputAutoCompleteTextView tvDescription;

    @BindView(R.id.text_input_ac_text_view_address_id)
    TextInputAutoCompleteTextView tvAddress;

    @BindView(R.id.button_add_address_id)
    Button buttonAddAdress;

    @BindView(R.id.recyclerView_media_id)
    RecyclerView recyclerView;

    @BindView(R.id.button_add_photo_id)
    Button buttonAddPhoto;

    @BindView(R.id.button_insert_listing_id)
    Button buttonInsertListing;

    /////////////////////////////////

    //RecyclerView Adapter
    private RVAdapterMediaHorizontal adapter;

    private RequestManager glide;

    private boolean accessInternalStorageGranted;

    private int counter;

    private Unbinder unbinder;

    private List<Bitmap> listOfBitmaps;

    private RealEstate realEstateCache;

    private List<ImageRealEstate> listOfImagesRealEstateCache;

    //InternetConnectionReceiver variables
    private InternetConnectionReceiver receiver;
    private IntentFilter intentFilter;
    private Snackbar snackbar;
    private boolean isInternetAvailable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: called!");

        this.app = (RealEstateManagerApp) getApplication();

        this.listOfBitmaps = new ArrayList<>();

        this.accessInternalStorageGranted = false;
        this.isInternetAvailable = false;

        this.counter = 0;

        this.getCacheInfo();

        ////////////////////////////////////////////////////////////////////////////////////////////
        setContentView(R.layout.activity_create_new_listing);
        setTitle("Create a New Listing");
        this.unbinder = ButterKnife.bind(this);

        this.glide = Glide.with(CreateNewListingActivity.this);

        this.updateViews();

        this.checkInternalStoragePermissionGranted();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: called");
        this.connectBroadcastReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.disconnectBroadcastReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: called!");
        this.disconnectBroadcastReceiver();
        unbinder.unbind();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: called!");
        // TODO: 19/08/2018 Might need to change this

        // TODO: 19/08/2018 Add a fragment saying, would you like to delete the media?
        // TODO: 19/08/2018 If yes, clean the list and the HashMap 
        //do nothing
    }

    @Override
    public void update(Observable o, Object internetAvailable) {
        Log.d(TAG, "update: called!");
        isInternetAvailable = Utils.setInternetAvailability(internetAvailable);
        snackbarConfiguration();
    }

    @OnClick ({R.id.button_add_address_id, R.id.button_add_photo_id, R.id.button_go_back_id, R.id.button_insert_listing_id})
    public void buttonClicked (View view) {
        Log.d(TAG, "buttonClicked: " + ((Button)view).getText().toString() + " clicked!");

        switch (view.getId()) {

            case R.id.button_add_address_id: {
                launchInsertAddressDialog();

            } break;

            case R.id.button_add_photo_id: {
                launchAddPhotoActivity();

            } break;

            case R.id.button_go_back_id: {
                Utils.launchActivity(this, MainActivity.class);

            }

            case R.id.button_insert_listing_id: {
                ToastHelper.toastShort(this, "Inserting Listing");

                insertListing();

                // TODO: 18/08/2018 Do all the checks
                // TODO: 19/08/2018 REMEMBER NOTIFICATION!

            } break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: called!");

        switch (requestCode) {

            case Constants.REQUEST_CODE_WRITE_EXTERNAL_STORAGE: {

                if (grantResults.length > 0 && grantResults[0] != -1) {
                    accessInternalStorageGranted = true;
                    getBitmapImagesFromTemporaryFiles();
                }
            }
            break;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void insertListing() {
        Log.d(TAG, "insertListing: called!");

//        RealEstate.Builder builder = new RealEstate.Builder();
//
//        FirebasePushIdGenerator.generate()
//
//        builder.setId(FirebasePushIdGenerator.generate());

        Utils.launchActivity(this, MainActivity.class);

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void launchAddPhotoActivity() {
        Log.d(TAG, "launchAddPhotoActivity: called!");

        if (!accessInternalStorageGranted) {
            ToastHelper.toastInternalStorageAccessNotGranted(this);

        } else {
            updateCache();
            Utils.launchActivity(this, AddPhotoActivity.class);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    //CACHE

    private void updateViews() {
        Log.d(TAG, "updateViews: called!");

        this.tvTypeOfBuilding.setText(realEstateCache.getType());
        this.tvPrice.setText(String.valueOf(realEstateCache.getPrice()));
        this.tvSurfaceArea.setText(String.valueOf(realEstateCache.getSurfaceArea()));
        this.tvNumberOfRooms.setText(String.valueOf(realEstateCache.getNumberOfRooms()));
        this.tvDescription.setText(realEstateCache.getDescription());
        this.tvAddress.setText(realEstateCache.getAddress());

    }

    private void updateCache() {
        Log.d(TAG, "updateCache: called!");

        this.updateStringValues();
        this.updateIntegerValues();
    }

    private void updateIntegerValues() {
        Log.d(TAG, "updateIntegerValues: called!");

        if (Utils.isInteger(tvPrice.getText().toString().trim())) {
            this.app.getRepository().getRealEstateCache().setPrice(Integer.valueOf(tvPrice.getText().toString().trim()));
        }

        if (Utils.isInteger(tvSurfaceArea.getText().toString().trim())) {
            this.app.getRepository().getRealEstateCache().setSurfaceArea(Integer.valueOf(tvSurfaceArea.getText().toString().trim()));
        }

        if (Utils.isInteger(tvNumberOfRooms.getText().toString().trim())) {
            this.app.getRepository().getRealEstateCache().setNumberOfRooms(Integer.valueOf(tvNumberOfRooms.getText().toString().trim()));
        }
    }

    private void updateStringValues() {
        Log.d(TAG, "updateStringValues: called!");

        this.app.getRepository().getRealEstateCache().setType(tvTypeOfBuilding.getText().toString().trim());
        this.app.getRepository().getRealEstateCache().setDescription(tvDescription.getText().toString().trim());
        this.app.getRepository().getRealEstateCache().setAddress(tvAddress.getText().toString().trim());
    }

    private void getCacheInfo() {
        Log.d(TAG, "getCacheInfo: called!");

        this.realEstateCache = app.getRepository().getRealEstateCache();
        this.listOfImagesRealEstateCache = app.getRepository().getListOfImagesRealEstateCache();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void launchInsertAddressDialog() {
        Log.d(TAG, "launchInsertAddressDialog: called!");

        DialogFragment dialog = new InsertAddressDialogFragment();
        dialog.show(getSupportFragmentManager(), "InsertAddressDialogFragment");

    }

    private void checkInternalStoragePermissionGranted() {
        Log.d(TAG, "checkInternalStoragePermissionGranted: called!");

        if (Utils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            accessInternalStorageGranted = true;
            getBitmapImagesFromTemporaryFiles();

        } else {
            Utils.requestPermission(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
        }
    }

    @SuppressLint("CheckResult")
    private void getBitmapImagesFromTemporaryFiles() {
        Log.d(TAG, "getBitmapImagesFromTemporaryFiles: called!");

        if (accessInternalStorageGranted) {

            String mainPath = app.getInternalStorage().getInternalFilesDirectory() + File.separator;
            String temporaryDir = mainPath + File.separator + Constants.TEMPORARY_DIRECTORY + File.separator;

            counter = listOfImagesRealEstateCache.size() - 1;

            for (int i = 0; i < listOfImagesRealEstateCache.size(); i++) {

                Single.just(app.getInternalStorage().readFile(temporaryDir + listOfImagesRealEstateCache.get(i).getId()))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableSingleObserver<byte[]>() {
                            @Override
                            public void onSuccess(byte[] data) {
                                Log.i(TAG, "onSuccess: called!");

                                listOfBitmaps.add(BitmapFactory.decodeByteArray(data, 0 , data.length));

                                counter--;
                                if (counter == 0) {
                                    configureRecyclerView();
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.i(TAG, "getBitmapImagesFromTemporaryFiles: called!");

                            }
                        });
            }
        }
    }

    private void configureRecyclerView() {
        Log.d(TAG, "configureRecyclerView: called!");

        this.recyclerView.setHasFixedSize(true);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false));
        this.adapter = new RVAdapterMediaHorizontal(
                this,
                listOfBitmaps,
                glide);
        this.recyclerView.setAdapter(this.adapter);

        this.configureOnClickRecyclerView();


    }

    private void configureOnClickRecyclerView () {
        Log.d(TAG, "configureOnClickRecyclerView: called!");

        ItemClickSupport.addTo(recyclerView)
                .setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        Log.d(TAG, "onItemClicked: item(" + position + ") clicked!");
                        ToastHelper.toastShort(CreateNewListingActivity.this, listOfImagesRealEstateCache.get(position).getDescription());
                    }
                });
    }

    @SuppressLint("CheckResult")
    private void checkAddressIsValid(final String street, final String city, final String postcode) {
        Log.d(TAG, "checkIfAddressIsValid: called!");

        if (isInternetAvailable) {

            GoogleServiceStreams.streamFetchPlaceFromText(street + "," + city + "," + postcode,
                    "textquery",
                    getResources().getString(R.string.a_k_p))
                    .subscribeWith(new DisposableObserver<PlaceFromText>() {
                        @Override
                        public void onNext(PlaceFromText placeFromText) {
                            Log.d(TAG, "onNext: called!");

                            if (Utils.checkPlaceFromTextIsNotNull(placeFromText)) {
                                ToastHelper.toastShort(CreateNewListingActivity.this, "The address is valid");

                                // TODO: 20/08/2018 Use PlaceId
                                Log.i(TAG, "onNext: " + placeFromText.getCandidates().get(0).getPlaceId());
                                setAddress(street,city,postcode);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.d(TAG, "onError: called!");
                        }

                        @Override
                        public void onComplete() {
                            Log.d(TAG, "onComplete: called!");
                        }
                    });

        } else {
            ToastHelper.toastShort(this, "Internet is not available, Address cannot be saved");

        }
    }

    private void setAddress (String street, String city, String postcode) {
        Log.d(TAG, "setAddress: called!");

        tvAddress.setText(street + ", " + city + ", " + postcode);
        buttonAddAdress.setText("Edit Address");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /** Method that connects a broadcastReceiver to the activity.
     * It allows to notify the user about the internet state
     * */
    private void connectBroadcastReceiver () {
        Log.d(TAG, "connectBroadcastReceiver: called!");

        receiver = new InternetConnectionReceiver();
        intentFilter = new IntentFilter(Constants.CONNECTIVITY_CHANGE_STATUS);
        Utils.connectReceiver(this, receiver, intentFilter, this);

    }

    /** Method that disconnects the broadcastReceiver from the activity.
     * */
    private void disconnectBroadcastReceiver () {
        Log.d(TAG, "disconnectBroadcastReceiver: called!");

        if (receiver != null) {
            Utils.disconnectReceiver(
                    this,
                    receiver,
                    this);
        }

        receiver = null;
        intentFilter = null;
        snackbar = null;

    }

    private void snackbarConfiguration () {
        Log.d(TAG, "snackbarConfiguration: called!");

        if (isInternetAvailable) {
            if (snackbar != null) {
                snackbar.dismiss();
            }

        } else {
            if (snackbar == null) {
                snackbar = Utils.createSnackbar(
                        this,
                        mainLayout,
                        getResources().getString(R.string.noInternet));

            } else {
                snackbar.show();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onDialogPositiveClick(DialogFragment dialogFragment, String street, String city, String postcode) {
        Log.d(TAG, "onDialogPositiveClick: called!");

        checkAddressIsValid(street,city,postcode);
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialogFragment) {
        Log.d(TAG, "onDialogNegativeClick: called!");

        ToastHelper.toastShort(this, "The address was not added");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

}
