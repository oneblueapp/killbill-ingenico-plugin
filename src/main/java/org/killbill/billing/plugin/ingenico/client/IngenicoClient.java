package org.killbill.billing.plugin.ingenico.client;

import com.ingenico.connect.gateway.sdk.java.Client;
import com.ingenico.connect.gateway.sdk.java.CommunicatorConfiguration;
import com.ingenico.connect.gateway.sdk.java.Factory;
import com.ingenico.connect.gateway.sdk.java.domain.definitions.Address;
import com.ingenico.connect.gateway.sdk.java.domain.definitions.AmountOfMoney;
import com.ingenico.connect.gateway.sdk.java.domain.definitions.CardWithoutCvv;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentRequest;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentResponse;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.*;
import com.ingenico.connect.gateway.sdk.java.domain.token.CreateTokenRequest;
import com.ingenico.connect.gateway.sdk.java.domain.token.CreateTokenResponse;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.CustomerToken;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.PersonalInformationToken;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.PersonalNameToken;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.TokenCard;
import com.ingenico.connect.gateway.sdk.java.domain.token.definitions.TokenCardData;

import org.killbill.billing.plugin.ingenico.client.model.PaymentData;
import org.killbill.billing.plugin.ingenico.client.model.PurchaseResult;
import org.killbill.billing.plugin.ingenico.client.model.SplitSettlementData;
import org.killbill.billing.plugin.ingenico.client.model.UserData;
import org.killbill.billing.plugin.ingenico.client.model.paymentinfo.Card;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 * Created by otaviosoares on 14/11/16.
 */
public class IngenicoClient implements Closeable {

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

    public PurchaseResult create(PaymentData<Card> paymentData, UserData userData, SplitSettlementData splitSettlementData) {
        Card paymentInfo = paymentData.getPaymentInfo();
        com.ingenico.connect.gateway.sdk.java.domain.definitions.Card card = new com.ingenico.connect.gateway.sdk.java.domain.definitions.Card();
        card.setCardNumber(paymentInfo.getNumber());
        card.setCardholderName(paymentInfo.getHolderName());
        card.setCvv(paymentInfo.getCvc());
        card.setExpiryDate(paymentInfo.getExpiryDate());

        CardPaymentMethodSpecificInput cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInput();
        cardPaymentMethodSpecificInput.setCard(card);
        cardPaymentMethodSpecificInput.setPaymentProductId(1);
        cardPaymentMethodSpecificInput.setSkipAuthentication(false);

        AmountOfMoney amountOfMoney = new AmountOfMoney();
        amountOfMoney.setAmount(paymentData.getAmount());
        amountOfMoney.setCurrencyCode(paymentData.getCurrency().name());

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
        customer.setLocale(userData.getShopperLocale().toLanguageTag());
        customer.setMerchantCustomerId(userData.getShopperReference());
        customer.setPersonalInformation(personalInformation);
        customer.setShippingAddress(shippingAddress);
        customer.setVatNumber(userData.getVatNumber());

        List<LineItem> items = new ArrayList<LineItem>();

        AmountOfMoney item1AmountOfMoney = new AmountOfMoney();
        item1AmountOfMoney.setAmount(2500L);
        item1AmountOfMoney.setCurrencyCode("EUR");

        LineItemInvoiceData item1InvoiceData = new LineItemInvoiceData();
        item1InvoiceData.setDescription("ACME Super Outfit");
        item1InvoiceData.setNrOfItems("1");
        item1InvoiceData.setPricePerItem(2500L);

        LineItem item1 = new LineItem();
        item1.setAmountOfMoney(item1AmountOfMoney);
        item1.setInvoiceData(item1InvoiceData);

        items.add(item1);

        AmountOfMoney item2AmountOfMoney = new AmountOfMoney();
        item2AmountOfMoney.setAmount(480L);
        item2AmountOfMoney.setCurrencyCode("EUR");

        LineItemInvoiceData item2InvoiceData = new LineItemInvoiceData();
        item2InvoiceData.setDescription("Asperin");
        item2InvoiceData.setNrOfItems("12");
        item2InvoiceData.setPricePerItem(40L);

        LineItem item2 = new LineItem();
        item2.setAmountOfMoney(item2AmountOfMoney);
        item2.setInvoiceData(item2InvoiceData);

        items.add(item2);

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
        order.setItems(items);
        order.setReferences(references);

        CreatePaymentRequest body = new CreatePaymentRequest();
        body.setCardPaymentMethodSpecificInput(cardPaymentMethodSpecificInput);
        body.setOrder(order);
        String merchantId = this.properties.getMerchantId();
        CreatePaymentResponse response = this.client.merchant(merchantId).payments().create(body);
        Payment paymentResponse = response.getPayment();
        PaymentOutput paymentOutput = paymentResponse.getPaymentOutput();


        final Map<String, String> formParams = new HashMap<String, String>();
//        formParams.put(AdyenPaymentPluginApi.PROPERTY_PA_REQ, result.getPaRequest());
//        formParams.put(AdyenPaymentPluginApi.PROPERTY_MD, result.getMd());
//        formParams.put(AdyenPaymentPluginApi.PROPERTY_DCC_AMOUNT_VALUE, result.getDccAmount() == null ? null : String.valueOf(result.getDccAmount().getValue()));
//        formParams.put(AdyenPaymentPluginApi.PROPERTY_DCC_AMOUNT_CURRENCY, result.getDccAmount() == null ? null : result.getDccAmount().getCurrency());
//        formParams.put(AdyenPaymentPluginApi.PROPERTY_DCC_SIGNATURE, result.getDccSignature());
//        formParams.put(AdyenPaymentPluginApi.PROPERTY_ISSUER_URL, result.getIssuerUrl());
//        formParams.putAll(extractMpiAdditionalData(result));
//        formParams.put(AdyenPaymentPluginApi.PROPERTY_TERM_URL, paymentData.getPaymentInfo().getTermUrl());

        return new PurchaseResult(
                merchantId,
                paymentResponse.getId(),
                paymentResponse.getStatus(),
                paymentOutput.getPaymentMethod(),
                paymentOutput.getReferences().getPaymentReference(),
                paymentOutput.getCardPaymentMethodSpecificOutput().getAuthorisationCode(),
                paymentOutput.getCardPaymentMethodSpecificOutput().getPaymentProductId(),
                null,
                null,
                paymentOutput.getCardPaymentMethodSpecificOutput().getFraudResults().getAvsResult(),
                paymentOutput.getCardPaymentMethodSpecificOutput().getFraudResults().getCvvResult(),
                paymentOutput.getCardPaymentMethodSpecificOutput().getFraudResults().getFraudServiceResult(),
                formParams);
    }

    public String tokenizeCreditCard(Card paymentInfo, UserData userData, final String cardAlias) {
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
        tokenCard.setAlias(cardAlias);

        CardWithoutCvv cardWithoutCvv = new CardWithoutCvv();
        cardWithoutCvv.setCardNumber(paymentInfo.getNumber());
        cardWithoutCvv.setCardholderName(paymentInfo.getHolderName());
        cardWithoutCvv.setExpiryDate(paymentInfo.getExpiryDate());

        TokenCardData tokenCardData = new TokenCardData();
        tokenCardData.setCardWithoutCvv(cardWithoutCvv);
        tokenCard.setData(tokenCardData);

        CreateTokenRequest body = new CreateTokenRequest();
        body.setCard(tokenCard);
        body.setPaymentProductId(0);

        CreateTokenResponse response = client.merchant(this.properties.getMerchantId()).tokens().create(body);
        return response.getToken();
    }
}
