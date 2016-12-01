/*
 * Copyright 2014 Groupon, Inc
 *
 * Groupon licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.ingenico.api;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginPaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.ingenico.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.ingenico.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.ingenico.client.model.PurchaseResult;
import org.killbill.billing.plugin.ingenico.client.payment.service.IngenicoCallErrorStatus;
import org.killbill.billing.plugin.ingenico.dao.IngenicoDao;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.records.IngenicoResponsesRecord;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;

public class IngenicoPaymentTransactionInfoPlugin extends PluginPaymentTransactionInfoPlugin {

    private static final int ERROR_CODE_MAX_LENGTH = 32;

    public IngenicoPaymentTransactionInfoPlugin(final UUID kbPaymentId,
                                                final UUID kbTransactionPaymentPaymentId,
                                                final TransactionType transactionType,
                                                final BigDecimal amount,
                                                final Currency currency,
                                                final DateTime utcNow,
                                                final PurchaseResult purchaseResult) {
        super(kbPaymentId,
              kbTransactionPaymentPaymentId,
              transactionType,
              amount,
              currency,
              getPaymentPluginStatus(purchaseResult.getIngenicoCallErrorStatus(), purchaseResult.getResult()),
              getGatewayError(purchaseResult),
              truncate(getGatewayErrorCode(purchaseResult)),
              purchaseResult.getPaymentId(),
              purchaseResult.getAuthorizationCode(),
              utcNow,
              utcNow,
              PluginProperties.buildPluginProperties(purchaseResult.getAdditionalData()));
    }

    public IngenicoPaymentTransactionInfoPlugin(final UUID kbPaymentId,
                                             final UUID kbTransactionPaymentPaymentId,
                                             final TransactionType transactionType,
                                             final BigDecimal amount,
                                             @Nullable final Currency currency,
                                             final Optional<PaymentServiceProviderResult> result,
                                             final DateTime utcNow,
                                             final PaymentModificationResponse paymentModificationResponse) {
        super(kbPaymentId,
              kbTransactionPaymentPaymentId,
              transactionType,
              amount,
              currency,
              getPaymentPluginStatus(paymentModificationResponse.getIngenicoCallErrorStatus(), result),
              getGatewayError(paymentModificationResponse),
              truncate(getGatewayErrorCode(paymentModificationResponse)),
              paymentModificationResponse.getPaymentId(),
              null,
              utcNow,
              utcNow,
              PluginProperties.buildPluginProperties(paymentModificationResponse.getAdditionalData()));
    }

    public IngenicoPaymentTransactionInfoPlugin(final IngenicoResponsesRecord record) {
        super(UUID.fromString(record.getKbPaymentId()),
              UUID.fromString(record.getKbPaymentTransactionId()),
              TransactionType.valueOf(record.getTransactionType()),
              record.getAmount(),
              Strings.isNullOrEmpty(record.getCurrency()) ? null : Currency.valueOf(record.getCurrency()),
              getPaymentPluginStatus(record),
              getGatewayError(record),
              truncate(getGatewayErrorCode(record)),
              record.getIngenicoPaymentId(),
              record.getIngenicoAuthorizationCode(),
              new DateTime(record.getCreatedDate(), DateTimeZone.UTC),
              new DateTime(record.getCreatedDate(), DateTimeZone.UTC),
              IngenicoModelPluginBase.buildPluginProperties(record.getAdditionalData()));
    }

    private static String getGatewayError(final PurchaseResult purchaseResult) {
        return purchaseResult.getErrorMessage() != null ? purchaseResult.getErrorMessage() : purchaseResult.getAdditionalData().get(PurchaseResult.EXCEPTION_MESSAGE);
    }

    private static String getGatewayError(final PaymentModificationResponse paymentModificationResponse) {
        return toString(paymentModificationResponse.getAdditionalData().get(PurchaseResult.EXCEPTION_MESSAGE));
    }

    private static String getGatewayError(final IngenicoResponsesRecord record) {
        return record.getIngenicoErrorMessage() != null ? record.getIngenicoErrorMessage() : toString(IngenicoDao.fromAdditionalData(record.getAdditionalData()).get(PurchaseResult.EXCEPTION_MESSAGE));
    }

    private static String getGatewayErrorCode(final PurchaseResult purchaseResult) {
        return purchaseResult.getErrorCode() != null ? purchaseResult.getErrorCode() : getExceptionClass(purchaseResult.getAdditionalData());
    }

    private static String getGatewayErrorCode(final PaymentModificationResponse paymentModificationResponse) {
        return paymentModificationResponse.getStatus() != null ? paymentModificationResponse.getStatus() : getExceptionClass(paymentModificationResponse.getAdditionalData());
    }

    private static String getGatewayErrorCode(final IngenicoResponsesRecord record) {
        if (record.getIngenicoErrorCode() != null) {
            return record.getIngenicoErrorCode();
        } else if (record.getIngenicoStatus() != null) {
            // PaymentModificationResponse
            return record.getIngenicoStatus();
        } else {
            return getExceptionClass(IngenicoDao.fromAdditionalData(record.getAdditionalData()));
        }
    }

    /**
     * Transforms ingenicoCallErrorStatus (where there any technical errors?) and pspResult (was the call successful from a business perspective) into the PaymentPluginStatus.
     * Therefor only one of the given params should be present (if there was a technical error we don't have a psp result and the other way around).
     */
    private static PaymentPluginStatus getPaymentPluginStatus(final Optional<IngenicoCallErrorStatus> ingenicoCallErrorStatus, final Optional<PaymentServiceProviderResult> result) {
        checkArgument(ingenicoCallErrorStatus.isPresent() ^ result.isPresent());
        return (result.isPresent()) ? resultToPaymentPluginStatus(result.get()) : ingenicoCallErrorStatusToPaymentPluginStatus(ingenicoCallErrorStatus.get());
    }

    private static PaymentPluginStatus getPaymentPluginStatus(final Optional<PaymentServiceProviderResult> result) {
        return (result.isPresent()) ? resultToPaymentPluginStatus(result.get()) : ingenicoCallErrorStatusToPaymentPluginStatus(IngenicoCallErrorStatus.UNKNOWN_FAILURE);
    }

    private static PaymentPluginStatus getPaymentPluginStatus(final IngenicoResponsesRecord record) {
        if (Strings.isNullOrEmpty(record.getIngenicoPaymentId())) {
            final String ingenicoCallErrorStatusString = toString(IngenicoDao.fromAdditionalData(record.getAdditionalData()).get(PurchaseResult.INGENICO_CALL_ERROR_STATUS));
            final IngenicoCallErrorStatus ingenicoCallErrorStatus;
            if (Strings.isNullOrEmpty(ingenicoCallErrorStatusString)) {
                ingenicoCallErrorStatus = IngenicoCallErrorStatus.UNKNOWN_FAILURE;
            } else {
                ingenicoCallErrorStatus = IngenicoCallErrorStatus.valueOf(ingenicoCallErrorStatusString);
            }
            return ingenicoCallErrorStatusToPaymentPluginStatus(ingenicoCallErrorStatus);
        } else {
            TransactionType transactionType = TransactionType.valueOf(record.getTransactionType());
            final PaymentServiceProviderResult paymentResult = PaymentServiceProviderResult.getPaymentResultForId(record.getIngenicoStatus() != null ? record.getIngenicoStatus() : record.getIngenicoResult(), transactionType);
            final Optional<PaymentServiceProviderResult> status = Optional.of(paymentResult);
            return getPaymentPluginStatus(status);
        }
    }

    private static PaymentPluginStatus ingenicoCallErrorStatusToPaymentPluginStatus(final IngenicoCallErrorStatus ingenicoCallErrorStatus) {
        switch (ingenicoCallErrorStatus) {
            case REQUEST_NOT_SEND:
                return PaymentPluginStatus.CANCELED;
            case RESPONSE_ABOUT_INVALID_REQUEST:
                return PaymentPluginStatus.CANCELED;
            case RESPONSE_NOT_RECEIVED:
                return PaymentPluginStatus.UNDEFINED;
            case RESPONSE_INVALID:
                return PaymentPluginStatus.UNDEFINED;
            case UNKNOWN_FAILURE:
                return PaymentPluginStatus.UNDEFINED;
            default:
                return PaymentPluginStatus.UNDEFINED;
        }
    }

    private static PaymentPluginStatus resultToPaymentPluginStatus(final PaymentServiceProviderResult result) {
        switch (result) {
            case REDIRECT_SHOPPER:
            case RECEIVED:
            case PENDING:
                return PaymentPluginStatus.PENDING;
            case AUTHORISED:
                return PaymentPluginStatus.PROCESSED;
            case REFUSED:
            case ERROR:
            case CANCELLED:
                return PaymentPluginStatus.ERROR;
            default:
                return PaymentPluginStatus.UNDEFINED;
        }
    }

    private static String toString(final Object obj) {
        return obj == null ? null : obj.toString();
    }

    private static String truncate(@Nullable final String string) {
        if (string == null) {
            return null;
        } else if (string.length() <= ERROR_CODE_MAX_LENGTH) {
            return string;
        } else {
            return string.substring(0, ERROR_CODE_MAX_LENGTH);
        }
    }

    private static String getExceptionClass(final Map additionalData) {
        final String fqClassName = toString(additionalData.get(PurchaseResult.EXCEPTION_CLASS));
        if (fqClassName == null || fqClassName.length() <= ERROR_CODE_MAX_LENGTH) {
            return fqClassName;
        } else {
            // Truncate the class name (thank you Logback! See TargetLengthBasedClassNameAbbreviator)
            return abbreviate(fqClassName, ERROR_CODE_MAX_LENGTH);
        }
    }

    private static String abbreviate(final String fqClassName, final int targetLength) {
        final StringBuilder buf = new StringBuilder(targetLength);
        final int[] dotIndexesArray = new int[16];
        final int[] lengthArray = new int[17];

        final int dotCount = computeDotIndexes(fqClassName, dotIndexesArray);
        if (dotCount == 0) {
            return fqClassName;
        }

        computeLengthArray(fqClassName, dotIndexesArray, lengthArray, dotCount);
        for (int i = 0; i <= dotCount; i++) {
            if (i == 0) {
                buf.append(fqClassName.substring(0, lengthArray[i] - 1));
            } else {
                buf.append(fqClassName.substring(dotIndexesArray[i - 1], dotIndexesArray[i - 1] + lengthArray[i]));
            }
        }

        return buf.toString();
    }

    private static int computeDotIndexes(final String className, final int[] dotArray) {
        int dotCount = 0;
        int k = 0;
        while (true) {
            k = className.indexOf(".", k);
            if (k != -1 && dotCount < dotArray.length) {
                dotArray[dotCount] = k;
                dotCount++;
                k++;
            } else {
                break;
            }
        }
        return dotCount;
    }

    private static void computeLengthArray(final String className, final int[] dotArray, final int[] lengthArray, final int dotCount) {
        int toTrim = className.length() - 32;

        int len;
        for (int i = 0; i < dotCount; i++) {
            int previousDotPosition = -1;
            if (i > 0) {
                previousDotPosition = dotArray[i - 1];
            }
            final int available = dotArray[i] - previousDotPosition - 1;

            if (toTrim > 0) {
                len = (available < 1) ? available : 1;
            } else {
                len = available;
            }
            toTrim -= (available - len);
            lengthArray[i] = len + 1;
        }

        final int lastDotIndex = dotCount - 1;
        lengthArray[dotCount] = className.length() - dotArray[lastDotIndex];
    }
}
