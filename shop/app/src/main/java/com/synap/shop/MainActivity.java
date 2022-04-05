package com.synap.shop;

import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.synap.pay.SynapPayButton;
import com.synap.pay.handler.EventHandler;
import com.synap.pay.handler.payment.SynapAuthorizeHandler;
import com.synap.pay.model.payment.SynapAddress;
import com.synap.pay.model.payment.SynapCardStorage;
import com.synap.pay.model.payment.SynapCountry;
import com.synap.pay.model.payment.SynapCurrency;
import com.synap.pay.model.payment.SynapDocument;
import com.synap.pay.model.payment.SynapExpiration;
import com.synap.pay.model.payment.SynapFeatures;
import com.synap.pay.model.payment.SynapOrder;
import com.synap.pay.model.payment.SynapPayment;
import com.synap.pay.model.payment.SynapPaymentCode;
import com.synap.pay.model.payment.SynapPerson;
import com.synap.pay.model.payment.SynapProduct;
import com.synap.pay.model.payment.SynapSettings;
import com.synap.pay.model.payment.SynapTransaction;
import com.synap.pay.model.payment.response.SynapAuthorizeResponse;
import com.synap.pay.model.security.SynapAuthenticator;
import com.synap.pay.theming.SynapLightTheme;
import com.synap.pay.theming.SynapTheme;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    private SynapPayButton paymentWidget;
    private FrameLayout synapForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Asocie y oculte el contenedor del formulario de pago (FrameLayout), hasta que se ejecute la acción de continuar al pago
        synapForm = findViewById(R.id.synapForm);
        synapForm.setVisibility(View.GONE);

        // Asocie el botón de continuar al pago (Button)
        Button startPayment=findViewById(R.id.startPaymentButton);
        startPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPayment();
            }
        });
    }

    private void startPayment(){
        // Muestre el contenedor del formulario de pago
        synapForm.setVisibility(View.VISIBLE);

        // Crea el objeto del widget de pago
        this.paymentWidget=SynapPayButton.createWithBanks(synapForm);

        // Seteo del ambiente ".SANDBOX" o ".PRODUCTION"
        SynapPayButton.setEnvironment(SynapPayButton.Environment.SANDBOX);

        // Seteo de los campos de transacción
        SynapTransaction transaction=this.buildTransaction();

        // Seteo de los campos de autenticación de seguridad
        SynapAuthenticator authenticator=this.buildAuthenticator(transaction);

        this.paymentWidget.configure(
                // Seteo de autenticación de seguridad y transacción
                authenticator,
                transaction,

                // Manejo de la respuesta
                new SynapAuthorizeHandler() {
                    @Override
                    public void success(SynapAuthorizeResponse response) {
                        Looper.prepare();
                        boolean resultSuccess = response.getSuccess();
                        if (resultSuccess) {
                            boolean resultAccepted=response.getResult().getAccepted();
                            String messageText=response.getResult().getMessage();
                            if (resultAccepted) {
                                // Agregue el código según la experiencia del cliente para la autorización
                                String paymentCode=response.getResult().getProcessorResult().getPaymentCode().getCode();
                                showMessage(messageText+" - Código: "+paymentCode);
                            }
                            else {
                                // Agregue el código según la experiencia del cliente para la denegación
                                showMessage(messageText);
                            }
                        }
                        else {
                            String messageText=response.getMessage().getText();
                            // Agregue el código de la experiencia que desee visualizar en un error
                            showMessage(messageText);
                        }
                        Looper.loop();
                    }
                    @Override
                    public void failed(SynapAuthorizeResponse response) {
                        Looper.prepare();
                        String messageText=response.getMessage().getText();
                        // Agregue el código de la experiencia que desee visualizar en un error
                        showMessage(messageText);
                        Looper.loop();
                    }
                }
        );
    }

    private SynapTransaction buildTransaction(){
        // Genere el número de orden (este es solo un ejemplo)
        String number=String.valueOf(System.currentTimeMillis());

        // Seteo de los datos de transacción
        // Referencie al objeto país
        SynapCountry country=new SynapCountry();
        // Seteo del código de país
        country.setCode("PER"); // Código de País (ISO 3166-2)

        // Referencie al objeto moneda
        SynapCurrency currency=new SynapCurrency();
        // Seteo del código de moneda
        currency.setCode("PEN"); // Código de Moneda - Alphabetic code (ISO 4217)

        //Seteo del monto
        String amount="1.00";

        // Referencie al objeto cliente
        SynapPerson customer=new SynapPerson();
        // Seteo del cliente
        customer.setName("Javier");
        customer.setLastName("Pérez");

        // Referencie al objeto dirección del cliente
        SynapAddress address=new SynapAddress();
        // Seteo del pais (country), niveles de ubicación geográfica (levels), dirección (line1 y line2) y código postal (zip)
        address.setCountry("PER"); // Código de País del cliente (ISO 3166-2)
        address.setLevels(new ArrayList<String>());
        address.getLevels().add("150000"); // Código de Área (Ubigeo - Departamento)
        address.getLevels().add("150100"); // Código de Área (Ubigeo - Provincia)
        address.getLevels().add("150101"); // Código de Área (Ubigeo - Distrito)
        address.setLine1("Ca Carlos Ferreyros 180"); // Dirección
        address.setZip("15036"); // Código Postal
        customer.setAddress(address);

        // Seteo del email y teléfono
        customer.setEmail("javier.perez@synapsolutions.com");
        customer.setPhone("999888777");

        // Referencie al objeto documento del cliente
        SynapDocument document=new SynapDocument();
        // Seteo del tipo y número de documento
        document.setType("DNI"); // [ DNI: Documento de identidad, CE: Carné de extranjería, PAS: Pasaporte, RUC: Registro único de contribuyente ]
        document.setNumber("44556677");
        customer.setDocument(document);

        // Seteo de los datos de envío
        SynapPerson shipping=customer; // Opcional, misma estructura que "customer"
        // Seteo de los datos de facturación
        SynapPerson billing=customer; // Opcional, misma estructura que "customer"

        // Referencie al objeto producto
        SynapProduct productItem=new SynapProduct();
        // Seteo de los datos de producto
        productItem.setCode("123"); // Opcional
        productItem.setName("Llavero");
        productItem.setQuantity("1"); // Opcional
        productItem.setUnitAmount("1.00"); // Opcional
        productItem.setAmount("1.00"); // Opcional

        // Referencie al objeto lista producto
        List<SynapProduct> products=new ArrayList<>();
        // Seteo de los datos de lista de producto
        products.add(productItem);

        // Referencie al objeto orden
        SynapOrder order=new SynapOrder();
        // Seteo de los datos de orden
        order.setNumber(number);
        order.setAmount(amount);
        order.setCountry(country);
        order.setCurrency(currency);
        order.setProducts(products);
        order.setCustomer(customer);
        order.setShipping(shipping);
        order.setBilling(billing);

        // Referencia al objeto pago
        SynapPayment payment=new SynapPayment();
        // Seteo de los datos de procesador
        SynapPaymentCode paymentCode=new SynapPaymentCode();
        paymentCode.setProcessorCode("KASHIO"); // [ KASHIO, PAGOEFECTIVO ]
        payment.setPaymentCode(paymentCode);

        // Referencie al objeto configuración
        SynapSettings settings=new SynapSettings();
        // Seteo de los datos de configuración
        settings.setBrands(Arrays.asList(new String[]{"BANKS"}));
        settings.setLanguage("es_PE");
        settings.setBusinessService("MOB");

        // Seteo de la fecha de expiración
        settings.setExpiration(new SynapExpiration());
        settings.getExpiration().setDate("2022-07-31T23:59:59.000Z"); // Máximo de 6 meses

        // Referencie al objeto transacción
        SynapTransaction transaction=new SynapTransaction();
        // Seteo de los datos de transacción
        transaction.setOrder(order);
        transaction.setSettings(settings);
        transaction.setPayment(payment);

        return transaction;
    }

    private SynapAuthenticator buildAuthenticator(SynapTransaction transaction){
        String apiKey="ab254a10-ddc2-4d84-8f31-d3fab9d49520"; //"4b099994-a876-4bbd-9ede-3c7115de0b7a"; //"5725e76a-1dd4-448c-8530-f7b1f3eb368b";

        // La signatureKey y la función de generación de firma debe usarse e implementarse en el servidor del comercio utilizando la función criptográfica SHA-512
        // solo con propósito de demostrar la funcionalidad, se implementará en el ejemplo
        // (bajo ninguna circunstancia debe exponerse la signatureKey y la función de firma desde la aplicación porque compromete la seguridad)
        String signatureKey="eDpehY%YPYgsoludCSZhu*WLdmKBWfAo"; //"Q1nPLoVPn@46cwmduy&%tZUWiLOovstc"; //"DzW/HDawaiwZ@OShFbbAWmcx0c3Aw217";

        // El campo onBehalf es opcional y se usa cuando un comercio agrupa otros sub comercios
        // la conexión con cada sub comercio se realiza con las credenciales del comercio agrupador
        // y enviando el identificador del sub comercio en el campo onBehalf
        //String onBehalf="cf747220-b471-4439-9130-d086d4ca83d4";

        String signature=generateSignature(transaction,apiKey,signatureKey);

        // Referencie el objeto de autenticación
        SynapAuthenticator authenticator=new SynapAuthenticator();

        // Seteo de identificador del comercio (apiKey)
        authenticator.setIdentifier(apiKey);

        // Seteo de firma, que permite verificar la integridad de la transacción
        authenticator.setSignature(signature);

        // Seteo de onBehalf, que permite identificar las transacciones de los subcomercios
        //authenticator.setOnBehalf(onBehalf);

        return authenticator;
    }

    // Muestra el mensaje de respuesta
    private void showMessage(String message){
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage(message);
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {
                        // Finaliza el intento de pago y regresa al inicio, el comercio define la experiencia del cliente
                        Handler looper = new Handler(getApplicationContext().getMainLooper());
                        looper.post(new Runnable() {
                            @Override
                            public void run() {
                                synapForm.setVisibility(View.VISIBLE);
                            }
                        });
                        dialog.cancel();
                    }
                }
        );

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    // La signatureKey y la función de generación de firma debe usarse e implementarse en el servidor del comercio utilizando la función criptográfica SHA-512
    // solo con propósito de demostrar la funcionalidad, se implementará en el ejemplo
    // (bajo ninguna circunstancia debe exponerse la signatureKey y la función de firma desde la aplicación porque compromete la seguridad)
    private String generateSignature(SynapTransaction transaction, String apiKey, String signatureKey){
        String orderNumber=transaction.getOrder().getNumber();
        String currencyCode=transaction.getOrder().getCurrency().getCode();
        String amount=transaction.getOrder().getAmount();

        String rawSignature=apiKey+orderNumber+currencyCode+amount+signatureKey;
        String signature=sha512Hex(rawSignature);
        return signature;
    }

    private String sha512Hex(String value){
        StringBuilder sb = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] bytes = md.digest(value.getBytes("UTF-8"));
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
