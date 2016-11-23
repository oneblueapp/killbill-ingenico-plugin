package org.killbill.billing.plugin.ingenico.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.killbill.billing.plugin.ingenico.client.model.PaymentData;
import org.killbill.billing.plugin.ingenico.client.model.PaymentInfo;
import org.killbill.billing.plugin.ingenico.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.ingenico.client.model.PurchaseResult;
import org.killbill.billing.plugin.ingenico.client.model.SplitSettlementData;
import org.killbill.billing.plugin.ingenico.client.model.UserData;
import org.killbill.billing.plugin.ingenico.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.ingenico.client.model.paymentinfo.Recurring;
import org.killbill.billing.plugin.ingenico.client.payment.builder.IngenicoRequestFactory;

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
    private IngenicoRequestFactory ingenicoRequestFactory;
    private final IngenicoConfigProperties properties;
    private Client client;

    public IngenicoClient(final IngenicoRequestFactory ingenicoRequestFactory, IngenicoConfigProperties properties) {
        this.ingenicoRequestFactory = ingenicoRequestFactory;
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

    public PurchaseResult create(PaymentData<Card> paymentData, UserData userData, final SplitSettlementData splitSettlementData) {
        PaymentInfo paymentInfo = paymentData.getPaymentInfo();

        AmountOfMoney amountOfMoney = new AmountOfMoney();
        amountOfMoney.setAmount(paymentData.getAmount());
        amountOfMoney.setCurrencyCode(paymentData.getCurrency().name());

        if (paymentInfo instanceof  Recurring) {
            Recurring recurring = (Recurring)paymentInfo;
            TokenizePaymentRequest body = new TokenizePaymentRequest();

            CreateTokenResponse response = client.merchant(this.properties.getMerchantId()).payments().tokenize(recurring.getRecurringDetailReference(), body);
            return null;
        }
        else if (paymentInfo instanceof  Card){
            CreatePaymentRequest body = ingenicoRequestFactory.createPaymentRequest(paymentData, userData, splitSettlementData);
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

        return null;
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
