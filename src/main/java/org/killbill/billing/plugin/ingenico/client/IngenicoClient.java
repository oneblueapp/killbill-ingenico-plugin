package org.killbill.billing.plugin.ingenico.client;

import com.ingenico.connect.gateway.sdk.java.Client;
import com.ingenico.connect.gateway.sdk.java.CommunicatorConfiguration;
import com.ingenico.connect.gateway.sdk.java.Factory;
import com.ingenico.connect.gateway.sdk.java.domain.definitions.Address;
import com.ingenico.connect.gateway.sdk.java.domain.definitions.AmountOfMoney;
import com.ingenico.connect.gateway.sdk.java.domain.definitions.Card;
import com.ingenico.connect.gateway.sdk.java.domain.definitions.CompanyInformation;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentRequest;
import com.ingenico.connect.gateway.sdk.java.domain.payment.CreatePaymentResponse;
import com.ingenico.connect.gateway.sdk.java.domain.payment.definitions.*;
import org.killbill.billing.plugin.ingenico.client.model.PaymentData;
import org.killbill.billing.plugin.ingenico.client.model.PurchaseResult;
import org.killbill.billing.plugin.ingenico.client.model.SplitSettlementData;
import org.killbill.billing.plugin.ingenico.client.model.UserData;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by otaviosoares on 14/11/16.
 */
public class IngenicoClient implements Closeable {

    private static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.ingenico.";
    private static final String PROPERTY_API_KEY = PROPERTY_PREFIX + "apiKey";
    private static final String PROPERTY_API_SECRET = PROPERTY_PREFIX + "apiSecret";
    private final Client client;

    public IngenicoClient(Properties properties) {
        this.client = createClient(properties);
    }

    private Client createClient(Properties properties) {
        CommunicatorConfiguration configuration = new CommunicatorConfiguration(properties)
            .withApiKeyId(properties.getProperty(PROPERTY_API_KEY))
            .withSecretApiKey(properties.getProperty(PROPERTY_API_SECRET));
        return Factory.createClient(configuration);
    }

    public PurchaseResult create(PaymentData paymentData, UserData userData, SplitSettlementData splitSettlementData) {
        Card card = new Card();
        card.setCardNumber("");
        card.setCardholderName("Wile E. Coyote");
        card.setCvv("123");
        card.setExpiryDate("1220");

        CardPaymentMethodSpecificInput cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInput();
        cardPaymentMethodSpecificInput.setCard(card);
        cardPaymentMethodSpecificInput.setPaymentProductId(1);
        cardPaymentMethodSpecificInput.setSkipAuthentication(false);

        AmountOfMoney amountOfMoney = new AmountOfMoney();
        amountOfMoney.setAmount(paymentData.getAmount());
        amountOfMoney.setCurrencyCode(paymentData.getCurrency().name());

        Address billingAddress = new Address();
        billingAddress.setAdditionalInfo("b");
        billingAddress.setCity("Monument Valley");
        billingAddress.setCountryCode("US");
        billingAddress.setHouseNumber("13");
        billingAddress.setState("Utah");
        billingAddress.setStreet("Desertroad");
        billingAddress.setZip("84536");

        CompanyInformation companyInformation = new CompanyInformation();
        companyInformation.setName("Acme Labs");

        ContactDetails contactDetails = new ContactDetails();
        contactDetails.setEmailAddress("wile.e.coyote@acmelabs.com");
        contactDetails.setEmailMessageType("html");
        contactDetails.setFaxNumber("+1234567891");
        contactDetails.setPhoneNumber("+1234567890");

        PersonalName name = new PersonalName();
        name.setFirstName(userData.getFirstName());
        name.setSurname(userData.getLastName());
        name.setSurnamePrefix("E.");
        name.setTitle("Mr.");

        PersonalInformation personalInformation = new PersonalInformation();
        personalInformation.setDateOfBirth(userData.getFormattedDateOfBirth("yyyyMMdd"));
        personalInformation.setGender(userData.getGender());
        personalInformation.setName(name);

        PersonalName shippingName = new PersonalName();
        shippingName.setFirstName("Road");
        shippingName.setSurname("Runner");
        shippingName.setTitle("Miss");

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
        customer.setCompanyInformation(companyInformation);
        customer.setContactDetails(contactDetails);
        customer.setLocale("en_US");
        customer.setMerchantCustomerId("1234");
        customer.setPersonalInformation(personalInformation);
        customer.setShippingAddress(shippingAddress);
        customer.setVatNumber("1234AB5678CD");

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
        CreatePaymentResponse response = this.client.merchant("").payments().create(body);

        return new PurchaseResult(response.getPayment().getStatus());
    }

    @Override
    public void close() throws IOException {
        this.client.close();
    }
}
