package com.shashi.service.impl;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import com.shashi.beans.CartBean;
import com.shashi.utility.MailMessage;

public class OrderServiceImplTest {

	@Test
	public void paymentSuccess_whenCartHasItems_multipliesPriceByQuantityAndTen() {
		CartBean item = new CartBean();
		item.setProdId("P1");
		item.setQuantity(2);
		item.setUserId("u1");

		try (MockedConstruction<CartServiceImpl> cartServiceMock = Mockito.mockConstruction(CartServiceImpl.class,
				(mock, context) -> {
					Mockito.when(mock.getAllCartItems("user@example.com")).thenReturn(Arrays.asList(item));
					Mockito.when(mock.removeAProduct("u1", "P1")).thenReturn(true);
				});
				MockedConstruction<ProductServiceImpl> productServiceMock = Mockito
						.mockConstruction(ProductServiceImpl.class, (mock, context) -> {
							Mockito.when(mock.getProductPrice("P1")).thenReturn(3.0);
							Mockito.when(mock.sellNProduct("P1", 2)).thenReturn(true);
						});
				MockedConstruction<OrderServiceImpl> orderServiceMock = Mockito
						.mockConstruction(OrderServiceImpl.class, (mock, context) -> {
							if (context.getCount() == 0) {
								return;
							}
							Mockito.when(mock.addTransaction(Mockito.any())).thenReturn(true);
						});
				MockedConstruction<UserServiceImpl> userServiceMock = Mockito.mockConstruction(UserServiceImpl.class,
						(mock, context) -> Mockito.when(mock.getFName("user@example.com")).thenReturn("Test"));
				org.mockito.MockedStatic<MailMessage> mailMessageMock = Mockito.mockStatic(MailMessage.class)) {

			OrderServiceImpl service = new OrderServiceImpl() {
				@Override
				public boolean addOrder(com.shashi.beans.OrderBean order) {
					assertEquals("P1", order.getProductId());
					assertEquals(2, order.getQuantity());
					assertEquals(3.0 * 2 * 10, order.getAmount(), 0.0001);
					return true;
				}
			};

			String status = service.paymentSuccess("user@example.com", 60.0);
			assertEquals("Order Placed Successfully!", status);
		}
	}

	@Test
	public void paymentSuccess_whenCartEmpty_returnsFailureStatus() {
		try (MockedConstruction<CartServiceImpl> cartServiceMock = Mockito.mockConstruction(CartServiceImpl.class,
				(mock, context) -> Mockito.when(mock.getAllCartItems("user@example.com")).thenReturn(Arrays.asList()))) {
			OrderServiceImpl service = new OrderServiceImpl();
			String status = service.paymentSuccess("user@example.com", 0.0);
			assertEquals("Order Placement Failed!", status);
		}
	}
}
