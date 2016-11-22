package org.killbill.billing.plugin.ingenico.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.killbill.billing.plugin.ingenico.client.model.PaymentData;
import org.killbill.billing.plugin.ingenico.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.ingenico.client.model.PurchaseResult;
import org.killbill.billing.plugin.ingenico.client.model.SplitSettlementData;
import org.killbill.billing.plugin.ingenico.client.model.UserData;
import org.killbill.billing.plugin.ingenico.client.model.paymentinfo.Card;

import com.ingenico.connect.gateway.sdk.java.ApiException;
import com.ingenico.connect.gateway.sdk.java.Client;
import com.ingenico.connect.gateway.sdk.java.CommunicatorConfiguration;
import com.ingenico.connect.gateway.sdk.java.DeclinedPaymentException;
import com.ingenico.connect.gateway.sdk.java.Factory;
import com.ingenico.connect.gateway.sdk.java.domain.definitions.Address;
import com.ingenico.connect.gateway.sdk.java.domain.definitions.AmountOfMoney;
import com.ingenico.connect.gateway.sdk.java.domain.definitions.CardWithoutCvv;
import com.ingenico.connect.gateway.sdk.java.domain.payment.ApprovePaymentRequest;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentRequest;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentResponse;
import com.ingenico.connect.gateway.sdk.java.domain.payment.PaymentApprovalResponse;
import com.ingenico.connect.gateway.sdk.java.domain.payment.TokenizePaymentRequest;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.AddressPersonal;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.ApprovePaymentNonSepaDirectDebitPaymentMethodSpecificInput;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.CardPaymentMethodSpecificInput;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.ContactDetails;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.Customer;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.Order;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.OrderApprovePayment;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.OrderInvoiceData;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.OrderReferences;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.OrderReferencesApprovePayment;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.Payment;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.PaymentOutput;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.PersonalInformation;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.PersonalName;
import com.ingenico.connect.gateway.sdk.java.domain.token.CreateTokenRequest;
import com.ingenico.connect.gateway.sdk.java.domain.token.CreateTokenResponse;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.CustomerToken;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.PersonalInformationToken;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.PersonalNameToken;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.TokenCard;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.TokenCardData;

/**
 * Created by otaviosoares on 14/11/16.
 */
public class IngenicoClient implements Closeable {

    private static final java.lang.String HMAC_ALGORITHM = "";
    private final IngenicoConfigProperties properties;
    private Client client;

    public IngenicoClient(IngenicoConfigProperties properties) {
        this.properties = properties;
        this.client = createClient(properties);
    }

    private Client createClient(IngenicoConfigProperties properties) {
        CommunicatorConfiguration configuration = new CommunicatorConfiguration(properties.toProperties())
                .withApiKeyId(properties.getApiKey())
                .withSecretApiKey(properties.getApiSecret());
        return Factory.createClient(configuration);
    }

    @Override
    public void close() throws IOException {
        this.client.close();
    }

    public PurchaseResult create(PaymentData<Card> paymentData, UserData userData) {
        Card paymentInfo = paymentData.getPaymentInfo();

        AmountOfMoney amountOfMoney = new AmountOfMoney();
        amountOfMoney.setAmount(paymentData.getAmount());
        amountOfMoney.setCurrencyCode(paymentData.getCurrency().name());

        if (paymentInfo.getToken() != null) {
            TokenizePaymentRequest body = new TokenizePaymentRequest();

            CreateTokenResponse response = client.merchant(this.properties.getMerchantId()).payments().tokenize(paymentInfo.getToken(), body);
            return null;
        }
        else {
            com.ingenico.connect.gateway.sdk.java.domain.definitions.Card card = new com.ingenico.connect.gateway.sdk.java.domain.definitions.Card();
            card.setCardNumber(paymentInfo.getNumber());
            card.setCardholderName(paymentInfo.getHolderName());
            card.setCvv(paymentInfo.getCvc());
            card.setExpiryDate(paymentInfo.getExpiryDate());

            CardPaymentMethodSpecificInput cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInput();
            cardPaymentMethodSpecificInput.setCard(card);
            cardPaymentMethodSpecificInput.setPaymentProductId(paymentInfo.getPaymentProductId());
            cardPaymentMethodSpecificInput.setSkipAuthentication(false);

            Address billingAddress = new Address();
            //        billingAddress.setAdditionalInfo("b");
            billingAddress.setCity(paymentInfo.getCity());
            billingAddress.setCountryCode(paymentInfo.getCountry());
            billingAddress.setHouseNumber(paymentInfo.getHouseNumberOrName());
            billingAddress.setState(paymentInfo.getStateOrProvince());
            billingAddress.setStreet(paymentInfo.getStreet());
            billingAddress.setZip(paymentInfo.getPostalCode());

    //        CompanyInformation companyInformation = new CompanyInformation();
    //        companyInformation.setName("Acme Labs");

            ContactDetails contactDetails = new ContactDetails();
            contactDetails.setEmailAddress(userData.getShopperEmail());
            contactDetails.setEmailMessageType("html");
    //        contactDetails.setFaxNumber("+1234567891");
            contactDetails.setPhoneNumber(userData.getTelephoneNumber());

            PersonalName name = new PersonalName();
            name.setFirstName(userData.getFirstName());
            name.setSurname(userData.getLastName());
    //        name.setSurnamePrefix("E.");
    //        name.setTitle("Mr.");

            PersonalInformation personalInformation = new PersonalInformation();
            personalInformation.setDateOfBirth(userData.getFormattedDateOfBirth("yyyyMMdd"));
            personalInformation.setGender(userData.getGender());
            personalInformation.setName(name);

            PersonalName shippingName = new PersonalName();
            shippingName.setFirstName("Road");
            shippingName.setSurname("Runner");
    //        shippingName.setTitle("Miss");

            AddressPersonal shippingAddress = new AddressPersonal();
            shippingAddress.setAdditionalInfo("Suite II");
            shippingAddress.setCity("Monument Valley");
            shippingAddress.setCountryCode("US");
            shippingAddress.setHouseNumber("1");
            shippingAddress.setName(shippingName);
            shippingAddress.setState("Utah");
            shippingAddress.setStreet("Desertroad");
            shippingAddress.setZip("84536");

            Customer customer = new Customer();
            customer.setBillingAddress(billingAddress);
    //        customer.setCompanyInformation(companyInformation);
            customer.setContactDetails(contactDetails);
            customer.setLocale(userData.getShopperLocale().toString());
            customer.setMerchantCustomerId(userData.getShopperReference());
            customer.setPersonalInformation(personalInformation);
            customer.setShippingAddress(shippingAddress);
            customer.setVatNumber(userData.getVatNumber());

//        List<LineItem> items = new ArrayList<LineItem>();
//
//        AmountOfMoney item1AmountOfMoney = new AmountOfMoney();
//        item1AmountOfMoney.setAmount(2500L);
//        item1AmountOfMoney.setCurrencyCode("EUR");
//
//        LineItemInvoiceData item1InvoiceData = new LineItemInvoiceData();
//        item1InvoiceData.setDescription("ACME Super Outfit");
//        item1InvoiceData.setNrOfItems("1");
//        item1InvoiceData.setPricePerItem(2500L);
//
//        LineItem item1 = new LineItem();
//        item1.setAmountOfMoney(item1AmountOfMoney);
//        item1.setInvoiceData(item1InvoiceData);
//
//        items.add(item1);
//
//        AmountOfMoney item2AmountOfMoney = new AmountOfMoney();
//        item2AmountOfMoney.setAmount(480L);
//        item2AmountOfMoney.setCurrencyCode("EUR");
//
//        LineItemInvoiceData item2InvoiceData = new LineItemInvoiceData();
//        item2InvoiceData.setDescription("Asperin");
//        item2InvoiceData.setNrOfItems("12");
//        item2InvoiceData.setPricePerItem(40L);
//
//        LineItem item2 = new LineItem();
//        item2.setAmountOfMoney(item2AmountOfMoney);
//        item2.setInvoiceData(item2InvoiceData);
//
//        items.add(item2);

            OrderInvoiceData invoiceData = new OrderInvoiceData();
            invoiceData.setInvoiceDate("20140306191500");
            invoiceData.setInvoiceNumber("000000123");

            OrderReferences references = new OrderReferences();
            references.setDescriptor("Fast and Furry-ous");
            references.setInvoiceData(invoiceData);
            references.setMerchantOrderId(123456L);
            references.setMerchantReference("AcmeOrder0001");

            Order order = new Order();
            order.setAmountOfMoney(amountOfMoney);
            order.setCustomer(customer);
            //        order.setItems(items);
            order.setReferences(references);

            CreatePaymentRequest body = new CreatePaymentRequest();
            body.setCardPaymentMethodSpecificInput(cardPaymentMethodSpecificInput);
            body.setOrder(order);
            String merchantId = this.properties.getMerchantId();
            CreatePaymentResponse response;
            response = this.client.merchant(merchantId).payments().create(body);
            try {
            } catch (DeclinedPaymentException e) {
                //handleDeclinedPayment(e.getCreatePaymentResult());
            } catch (ApiException e) {
                //handleApiErrors(e.getErrors());
            }

            Payment paymentResponse = response.getPayment();
            PaymentOutput paymentOutput = paymentResponse.getPaymentOutput();


            final Map<String, String> additionalData = new HashMap<String, String>();

            return new PurchaseResult(
                    merchantId,
                    paymentResponse.getId(),
                    paymentResponse.getStatus(),
                    paymentOutput.getPaymentMethod(),
                    paymentOutput.getReferences().getMerchantReference(),
                    paymentOutput.getCardPaymentMethodSpecificOutput().getAuthorisationCode(),
                    paymentOutput.getCardPaymentMethodSpecificOutput().getPaymentProductId(),
                    paymentOutput.getCardPaymentMethodSpecificOutput().getFraudResults().getAvsResult(),
                    paymentOutput.getCardPaymentMethodSpecificOutput().getFraudResults().getCvvResult(),
                    paymentOutput.getCardPaymentMethodSpecificOutput().getFraudResults().getFraudServiceResult(),
                    additionalData);
        }
    }

    public PaymentModificationResponse capture(final PaymentData paymentData, final SplitSettlementData splitSettlementData) {
        ApprovePaymentNonSepaDirectDebitPaymentMethodSpecificInput directDebitPaymentMethodSpecificInput = new ApprovePaymentNonSepaDirectDebitPaymentMethodSpecificInput();
        directDebitPaymentMethodSpecificInput.setDateCollect("20150201");
        directDebitPaymentMethodSpecificInput.setToken("bfa8a7e4-4530-455a-858d-204ba2afb77e");

        OrderReferencesApprovePayment references = new OrderReferencesApprovePayment();
        references.setMerchantReference("AcmeOrder0001");

        OrderApprovePayment order = new OrderApprovePayment();
        order.setReferences(references);

        ApprovePaymentRequest body = new ApprovePaymentRequest();
        body.setAmount(paymentData.getAmount());
        body.setDirectDebitPaymentMethodSpecificInput(directDebitPaymentMethodSpecificInput);
        body.setOrder(order);

        PaymentApprovalResponse response = client.merchant(this.properties.getMerchantId()).payments().approve("paymentId", body);
        return null;
    }

    public String tokenizeCreditCard(Card paymentInfo, UserData userData) {
        Address billingAddress = new Address();
        //billingAddress.setAdditionalInfo("Suite II");
        billingAddress.setCity(paymentInfo.getCity());
        billingAddress.setCountryCode(paymentInfo.getCountry());
        billingAddress.setHouseNumber(paymentInfo.getHouseNumberOrName());
        billingAddress.setState(paymentInfo.getStateOrProvince());
        billingAddress.setStreet(paymentInfo.getStreet());
        billingAddress.setZip(paymentInfo.getPostalCode());

//        CompanyInformation companyInformation = new CompanyInformation();
//        companyInformation.setName("Acme Labs");

        PersonalNameToken name = new PersonalNameToken();
        name.setFirstName(userData.getFirstName());
        name.setSurname(userData.getLastName());

        PersonalInformationToken personalInformation = new PersonalInformationToken();
        personalInformation.setName(name);

        CustomerToken customer = new CustomerToken();
        customer.setBillingAddress(billingAddress);
//        customer.setCompanyInformation(companyInformation);
        customer.setMerchantCustomerId(userData.getShopperReference());
        customer.setPersonalInformation(personalInformation);

        TokenCard tokenCard = new TokenCard();
        tokenCard.setCustomer(customer);

        CardWithoutCvv cardWithoutCvv = new CardWithoutCvv();
        cardWithoutCvv.setCardNumber(paymentInfo.getNumber());
        cardWithoutCvv.setCardholderName(paymentInfo.getHolderName());
        cardWithoutCvv.setExpiryDate(paymentInfo.getExpiryDate());

        TokenCardData tokenCardData = new TokenCardData();
        tokenCardData.setCardWithoutCvv(cardWithoutCvv);
        tokenCard.setData(tokenCardData);

        CreateTokenRequest body = new CreateTokenRequest();
        body.setCard(tokenCard);
        body.setPaymentProductId(paymentInfo.getPaymentProductId());

        try {
            final CreateTokenResponse response = client.merchant(this.properties.getMerchantId()).tokens().create(body);
            return response.getToken();
        }
        catch (Exception e) {
            return null;
        }
    }

    public PaymentModificationResponse cancel(final String merchantAccount, final PaymentData paymentData, final String pspReference, final SplitSettlementData splitSettlementData) {
        return null;
    }

    public PaymentModificationResponse refund(final String merchantAccount, final PaymentData paymentData, final String pspReference, final SplitSettlementData splitSettlementData) {
        return null;
    }

    public PurchaseResult credit(final PaymentData paymentData, final SplitSettlementData splitSettlementData) {
        return null;
    }
}
