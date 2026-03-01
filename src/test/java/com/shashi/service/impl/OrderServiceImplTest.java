package com.shashi.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import com.shashi.beans.CartBean;
import com.shashi.beans.OrderBean;
import com.shashi.beans.TransactionBean;

public class OrderServiceImplTest {

	@Test
	public void paymentSuccess_whenCartEmpty_shouldFail() {
		try (MockedConstruction<CartServiceImpl> ignored = Mockito.mockConstruction(CartServiceImpl.class,
				(mock, context) -> Mockito.when(mock.getAllCartItems(anyString())).thenReturn(Collections.emptyList()))) {

			String status = new OrderServiceImpl().paymentSuccess("user@example.com", 123.0);
			assertEquals("Order Placement Failed!", status);
		}
	}

	@Test
	public void paymentSuccess_shouldMultiplyLineAmountBy10_andReturnSuccess() {
		CartBean cartItem = Mockito.mock(CartBean.class);
		Mockito.when(cartItem.getProdId()).thenReturn("P100");
		Mockito.when(cartItem.getQuantity()).thenReturn(2);
		Mockito.when(cartItem.getUserId()).thenReturn("user@example.com");

		double price = 5.0;
		double expectedAmount = price * 2 * 10;

		ArgumentCaptor<OrderBean> orderCaptor = ArgumentCaptor.forClass(OrderBean.class);

		try (MockedConstruction<CartServiceImpl> ignored1 = Mockito.mockConstruction(CartServiceImpl.class, (mock, context) -> {
			Mockito.when(mock.getAllCartItems(anyString())).thenReturn(Arrays.asList(cartItem));
			Mockito.when(mock.removeAProduct(anyString(), anyString())).thenReturn(true);
		});

				MockedConstruction<ProductServiceImpl> ignored2 = Mockito.mockConstruction(ProductServiceImpl.class, (mock, context) -> {
					Mockito.when(mock.getProductPrice(anyString())).thenReturn(price);
					Mockito.when(mock.sellNProduct(anyString(), anyInt())).thenReturn(true);
				});

				MockedConstruction<UserServiceImpl> ignored3 = Mockito.mockConstruction(UserServiceImpl.class,
						(mock, context) -> Mockito.when(mock.getFName(anyString())).thenReturn("Test"));

				MockedConstruction<OrderServiceImpl> ignored4 = Mockito.mockConstruction(OrderServiceImpl.class, (mock, context) -> {
					Mockito.when(mock.addOrder(orderCaptor.capture())).thenReturn(true);
					Mockito.when(mock.addTransaction(any(TransactionBean.class))).thenReturn(true);
				});

				MockedConstruction<TransactionBean> ignored5 = Mockito.mockConstruction(TransactionBean.class, (mock, context) -> {
					Mockito.when(mock.getTransactionId()).thenReturn("T1");
					Mockito.when(mock.getTransAmount()).thenReturn(123.0);
				})) {

			String status = new OrderServiceImpl().paymentSuccess("user@example.com", 123.0);
			assertEquals("Order Placed Successfully!", status);
		}

		OrderBean captured = orderCaptor.getValue();
		assertEquals("P100", captured.getProductId());
		assertEquals(2, captured.getQuantity());
		assertEquals(expectedAmount, captured.getAmount(), 0.0001);
	}
}
