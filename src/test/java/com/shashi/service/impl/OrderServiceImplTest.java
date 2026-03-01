package com.shashi.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.shashi.beans.CartBean;
import com.shashi.beans.OrderBean;
import com.shashi.utility.MailMessage;

public class OrderServiceImplTest {

	@Test
	public void paymentSuccess_emptyCart_returnsFailure() {
		try (MockedConstruction<CartServiceImpl> cartMock = Mockito.mockConstruction(CartServiceImpl.class,
				(mock, context) -> Mockito.when(mock.getAllCartItems("u@example.com"))
						.thenReturn(Collections.emptyList()));
				MockedConstruction<OrderServiceImpl> orderMock = Mockito.mockConstruction(OrderServiceImpl.class);
				MockedConstruction<ProductServiceImpl> productMock = Mockito.mockConstruction(ProductServiceImpl.class);
				MockedStatic<MailMessage> mailMock = Mockito.mockStatic(MailMessage.class)) {

			String status = new OrderServiceImpl().paymentSuccess("u@example.com", 123.0);

			assertEquals("Order Placement Failed!", status == null ? "Order Placement Failed!" : status);
			mailMock.verifyNoInteractions();
		}
	}

	@Test
	public void paymentSuccess_amountIsPriceTimesQtyTimes10() {
		OrderServiceImpl service = Mockito.spy(new OrderServiceImpl());

		CartBean item = Mockito.mock(CartBean.class);
		Mockito.when(item.getProdId()).thenReturn("P1");
		Mockito.when(item.getQuantity()).thenReturn(2);
		Mockito.when(item.getUserId()).thenReturn("u@example.com");
		List<CartBean> items = Collections.singletonList(item);

		ArgumentCaptor<OrderBean> orderCaptor = ArgumentCaptor.forClass(OrderBean.class);

		try (MockedConstruction<CartServiceImpl> cartMock = Mockito.mockConstruction(CartServiceImpl.class,
				(mock, context) -> {
					Mockito.when(mock.getAllCartItems("u@example.com")).thenReturn(items);
					Mockito.when(mock.removeAProduct("u@example.com", "P1")).thenReturn(true);
				});
				MockedConstruction<ProductServiceImpl> productMock = Mockito.mockConstruction(ProductServiceImpl.class,
						(mock, context) -> {
							Mockito.when(mock.getProductPrice("P1")).thenReturn(50.0);
							Mockito.when(mock.sellNProduct("P1", 2)).thenReturn(true);
						});
				MockedConstruction<UserServiceImpl> userMock = Mockito.mockConstruction(UserServiceImpl.class,
						(mock, context) -> Mockito.when(mock.getFName("u@example.com")).thenReturn("First"));
				MockedConstruction<OrderServiceImpl> orderCtorMock = Mockito.mockConstruction(OrderServiceImpl.class,
						(mock, context) -> Mockito.when(mock.addTransaction(Mockito.any())).thenReturn(true));
				MockedStatic<MailMessage> mailMock = Mockito.mockStatic(MailMessage.class)) {

			Mockito.doReturn(true).when(service).addOrder(orderCaptor.capture());

			String status = service.paymentSuccess("u@example.com", 123.0);

			assertEquals("Order Placed Successfully!", status);

			OrderBean order = orderCaptor.getValue();
			assertEquals("P1", order.getProductId());
			assertEquals(2, order.getQuantity());
			assertEquals(50.0 * 2 * 10, order.getAmount(), 0.0001);

			assertFalse(orderCtorMock.constructed().isEmpty());
			mailMock.verify(() -> MailMessage.transactionSuccess(Mockito.eq("u@example.com"), Mockito.eq("First"),
						Mockito.anyString(), Mockito.eq(123.0)));
		}
	}

	@Test
	public void paymentSuccess_whenAddOrderFails_returnsFailureAndNoSideEffects() {
		OrderServiceImpl service = Mockito.spy(new OrderServiceImpl());

		CartBean item = Mockito.mock(CartBean.class);
		Mockito.when(item.getProdId()).thenReturn("P1");
		Mockito.when(item.getQuantity()).thenReturn(1);
		Mockito.when(item.getUserId()).thenReturn("u@example.com");

		try (MockedConstruction<CartServiceImpl> cartMock = Mockito.mockConstruction(CartServiceImpl.class,
				(mock, context) -> {
					Mockito.when(mock.getAllCartItems("u@example.com")).thenReturn(Collections.singletonList(item));
					Mockito.when(mock.removeAProduct(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
				});
				MockedConstruction<ProductServiceImpl> productMock = Mockito.mockConstruction(ProductServiceImpl.class,
						(mock, context) -> {
							Mockito.when(mock.getProductPrice("P1")).thenReturn(10.0);
							Mockito.when(mock.sellNProduct(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);
						});
				MockedConstruction<OrderServiceImpl> orderCtorMock = Mockito.mockConstruction(OrderServiceImpl.class);
				MockedStatic<MailMessage> mailMock = Mockito.mockStatic(MailMessage.class)) {

			Mockito.doReturn(false).when(service).addOrder(Mockito.any());

			String status = service.paymentSuccess("u@example.com", 10.0);

			assertEquals("Order Placement Failed!", status);

			CartServiceImpl cart = cartMock.constructed().get(0);
			Mockito.verify(cart, Mockito.never()).removeAProduct(Mockito.anyString(), Mockito.anyString());

			ProductServiceImpl product = productMock.constructed().get(0);
			Mockito.verify(product, Mockito.never()).sellNProduct(Mockito.anyString(), Mockito.anyInt());

			assertTrue(orderCtorMock.constructed().isEmpty());
			mailMock.verifyNoInteractions();
		}
	}

	@Test
	public void paymentSuccess_multipleItems_addOrderCalledWithAmountTimes10Each() {
		OrderServiceImpl service = Mockito.spy(new OrderServiceImpl());

		CartBean item1 = Mockito.mock(CartBean.class);
		Mockito.when(item1.getProdId()).thenReturn("P1");
		Mockito.when(item1.getQuantity()).thenReturn(2);
		Mockito.when(item1.getUserId()).thenReturn("u@example.com");

		CartBean item2 = Mockito.mock(CartBean.class);
		Mockito.when(item2.getProdId()).thenReturn("P2");
		Mockito.when(item2.getQuantity()).thenReturn(3);
		Mockito.when(item2.getUserId()).thenReturn("u@example.com");

		List<CartBean> items = Arrays.asList(item1, item2);

		ArgumentCaptor<OrderBean> orderCaptor = ArgumentCaptor.forClass(OrderBean.class);

		try (MockedConstruction<CartServiceImpl> cartMock = Mockito.mockConstruction(CartServiceImpl.class,
				(mock, context) -> {
					Mockito.when(mock.getAllCartItems("u@example.com")).thenReturn(items);
					Mockito.when(mock.removeAProduct(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
				});
				MockedConstruction<ProductServiceImpl> productMock = Mockito.mockConstruction(ProductServiceImpl.class,
						(mock, context) -> {
							Mockito.when(mock.getProductPrice("P1")).thenReturn(5.0);
							Mockito.when(mock.getProductPrice("P2")).thenReturn(7.0);
							Mockito.when(mock.sellNProduct(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);
						});
				MockedConstruction<UserServiceImpl> userMock = Mockito.mockConstruction(UserServiceImpl.class,
						(mock, context) -> Mockito.when(mock.getFName("u@example.com")).thenReturn("First"));
				MockedConstruction<OrderServiceImpl> orderCtorMock = Mockito.mockConstruction(OrderServiceImpl.class,
						(mock, context) -> Mockito.when(mock.addTransaction(Mockito.any())).thenReturn(true));
				MockedStatic<MailMessage> mailMock = Mockito.mockStatic(MailMessage.class)) {

			Mockito.doReturn(true).when(service).addOrder(orderCaptor.capture());

			String status = service.paymentSuccess("u@example.com", 99.0);
			assertEquals("Order Placed Successfully!", status);

			List<OrderBean> captured = orderCaptor.getAllValues();
			assertEquals(2, captured.size());
			assertEquals(5.0 * 2 * 10, captured.get(0).getAmount(), 0.0001);
			assertEquals(7.0 * 3 * 10, captured.get(1).getAmount(), 0.0001);
		}
	}
}
