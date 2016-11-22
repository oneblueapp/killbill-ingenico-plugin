/*
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
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

package org.killbill.billing.plugin.ingenico.client.model;

public class PaymentInfo {
    private String shopperInteraction;
    private String shopperStatement;

    // Billing Address
    private String street;
    private String houseNumberOrName;
    private String city;
    private String postalCode;
    private String stateOrProvince;
    private String country;
    // Special fields
    private String acquirer;
    private String acquirerMID;
    private Integer paymentProductId;

    public String getShopperInteraction() {
        return shopperInteraction;
    }

    public void setShopperInteraction(final String shopperInteraction) {
        this.shopperInteraction = shopperInteraction;
    }

    public String getShopperStatement() {
        return shopperStatement;
    }

    public void setShopperStatement(final String shopperStatement) {
        this.shopperStatement = shopperStatement;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(final String street) {
        this.street = street;
    }

    public String getHouseNumberOrName() {
        return houseNumberOrName;
    }

    public void setHouseNumberOrName(final String houseNumberOrName) {
        this.houseNumberOrName = houseNumberOrName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(final String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(final String postalCode) {
        this.postalCode = postalCode;
    }

    public String getStateOrProvince() {
        return stateOrProvince;
    }

    public void setStateOrProvince(final String stateOrProvince) {
        this.stateOrProvince = stateOrProvince;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public Integer getPaymentProductId() {
        return paymentProductId;
    }

    public void setPaymentProductId(final Integer paymentProductId) {
        this.paymentProductId = paymentProductId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PaymentInfo{");
        sb.append(", shopperInteraction='").append(shopperInteraction).append('\'');
        sb.append(", shopperStatement='").append(shopperStatement).append('\'');
        sb.append(", street='").append(street).append('\'');
        sb.append(", houseNumberOrName='").append(houseNumberOrName).append('\'');
        sb.append(", city='").append(city).append('\'');
        sb.append(", postalCode='").append(postalCode).append('\'');
        sb.append(", stateOrProvince='").append(stateOrProvince).append('\'');
        sb.append(", country='").append(country).append('\'');
        sb.append(", acquirer='").append(acquirer).append('\'');
        sb.append(", acquirerMID='").append(acquirerMID).append('\'');
        sb.append(", paymentProductId='").append(paymentProductId).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PaymentInfo that = (PaymentInfo) o;

        if (shopperInteraction != null ? !shopperInteraction.equals(that.shopperInteraction) : that.shopperInteraction != null) {
            return false;
        }
        if (shopperStatement != null ? !shopperStatement.equals(that.shopperStatement) : that.shopperStatement != null) {
            return false;
        }
        if (street != null ? !street.equals(that.street) : that.street != null) {
            return false;
        }
        if (houseNumberOrName != null ? !houseNumberOrName.equals(that.houseNumberOrName) : that.houseNumberOrName != null) {
            return false;
        }
        if (city != null ? !city.equals(that.city) : that.city != null) {
            return false;
        }
        if (postalCode != null ? !postalCode.equals(that.postalCode) : that.postalCode != null) {
            return false;
        }
        if (stateOrProvince != null ? !stateOrProvince.equals(that.stateOrProvince) : that.stateOrProvince != null) {
            return false;
        }
        if (country != null ? !country.equals(that.country) : that.country != null) {
            return false;
        }
        if (acquirer != null ? !acquirer.equals(that.acquirer) : that.acquirer != null) {
            return false;
        }
        if (acquirerMID != null ? !acquirerMID.equals(that.acquirerMID) : that.acquirerMID != null) {
            return false;
        }
        return paymentProductId != null ? paymentProductId.equals(that.paymentProductId) : that.paymentProductId == null;

    }

    @Override
    public int hashCode() {
        int result = shopperInteraction != null ? shopperInteraction.hashCode() : 0;
        result = 31 * result + (shopperStatement != null ? shopperStatement.hashCode() : 0);
        result = 31 * result + (street != null ? street.hashCode() : 0);
        result = 31 * result + (houseNumberOrName != null ? houseNumberOrName.hashCode() : 0);
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (postalCode != null ? postalCode.hashCode() : 0);
        result = 31 * result + (stateOrProvince != null ? stateOrProvince.hashCode() : 0);
        result = 31 * result + (country != null ? country.hashCode() : 0);
        result = 31 * result + (acquirer != null ? acquirer.hashCode() : 0);
        result = 31 * result + (acquirerMID != null ? acquirerMID.hashCode() : 0);
        result = 31 * result + (paymentProductId != null ? paymentProductId.hashCode() : 0);
        return result;
    }
}
