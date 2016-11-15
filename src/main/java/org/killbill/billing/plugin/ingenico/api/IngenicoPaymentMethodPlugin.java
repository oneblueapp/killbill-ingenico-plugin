package org.killbill.billing.plugin.ingenico.api;

import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.plugin.api.payment.PluginPaymentMethodPlugin;
import org.killbill.billing.plugin.ingenico.dao.IngenicoDao;
import org.killbill.billing.plugin.ingenico.dao.gen.tables.records.IngenicoPaymentMethodsRecord;

import java.util.UUID;

/**
 * Created by otaviosoares on 15/11/16.
 */
public class IngenicoPaymentMethodPlugin extends PluginPaymentMethodPlugin {
    public IngenicoPaymentMethodPlugin(IngenicoPaymentMethodsRecord record) {
        super(record.getKbPaymentMethodId() == null ? null : UUID.fromString(record.getKbPaymentMethodId()),
                record.getToken(),
                (record.getIsDefault() != null) && IngenicoDao.TRUE == record.getIsDefault(),
                IngenicoModelPluginBase.buildPluginProperties(record.getAdditionalData()));
    }
}
