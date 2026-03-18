package fr.ailegalcase.billing;

import com.stripe.exception.AuthenticationException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeCustomerServiceTest {

    // U-01 : Stripe désactivé → Optional.empty(), pas d'appel SDK
    @Test
    void createCustomer_stripeDisabled_returnsEmpty() {
        StripeCustomerService service = new StripeCustomerService(false, "");

        Optional<String> result = service.createCustomer("user@test.com", UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    // U-02 : Stripe activé, succès → retourne customer id
    @Test
    void createCustomer_stripeEnabled_success_returnsCustomerId() throws Exception {
        StripeCustomerService service = new StripeCustomerService(true, "sk_test_fake");

        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getId()).thenReturn("cus_abc123");

        try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class)) {
            customerStatic.when(() -> Customer.create(any(CustomerCreateParams.class)))
                    .thenReturn(mockCustomer);

            Optional<String> result = service.createCustomer("user@test.com", UUID.randomUUID());

            assertThat(result).contains("cus_abc123");
        }
    }

    // U-03 : Stripe activé, exception → fail-open, Optional.empty()
    @Test
    void createCustomer_stripeEnabled_exception_returnsEmpty() throws Exception {
        StripeCustomerService service = new StripeCustomerService(true, "sk_test_fake");

        try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class)) {
            customerStatic.when(() -> Customer.create(any(CustomerCreateParams.class)))
                    .thenThrow(new AuthenticationException("Invalid API key", "req_123", null, 401));

            Optional<String> result = service.createCustomer("user@test.com", UUID.randomUUID());

            assertThat(result).isEmpty();
        }
    }
}
