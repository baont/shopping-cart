package com.shashi.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import com.shashi.beans.CartBean;
import com.shashi.beans.OrderBean;
import com.shashi.utility.MailMessage;

public class OrderServiceImplTest {

	@Test
	public void paymentSuccess_emptyCart_returnsFailure() {
		try (MockedConstruction<CartServiceImpl> cartSvcConstruction = mockConstruction(CartServiceImpl.class,
				(mock, context) -> doReturn(Collections.emptyList()).when(mock).getAllCartItems("u"));
				MockedConstruction<ProductServiceImpl> prodSvcConstruction = mockConstruction(ProductServiceImpl.class);
				MockedStatic<MailMessage> mailMessage = mockStatic(MailMessage.class)) {

			String status = new OrderServiceImpl().paymentSuccess("u", 123.0);
			assertEquals("Order Placement Failed!", status);

			verify(cartSvcConstruction.constructed().get(0), times(1)).getAllCartItems("u");
			mailMessage.verifyNoInteractions();
			assertEquals(0, prodSvcConstruction.constructed().size());
		}
	}

	@Test
	public void paymentSuccess_amountIncludesFactor10() {
		CartBean item = new CartBean("u", "p1", 2);

		try (MockedConstruction<CartServiceImpl> cartSvcConstruction = mockConstruction(CartServiceImpl.class,
				(mock, context) -> {
					doReturn(Arrays.asList(item)).when(mock).getAllCartItems("u");
					doReturn(true).when(mock).removeAProduct("u", "p1");
				});
				MockedConstruction<ProductServiceImpl> prodSvcConstruction = mockConstruction(ProductServiceImpl.class,
						(mock, context) -> {
							doReturn(5.0).when(mock).getProductPrice("p1");
							doReturn(true).when(mock).sellNProduct("p1", 2);
						});
				MockedConstruction<UserServiceImpl> userSvcConstruction = mockConstruction(UserServiceImpl.class,
						(mock, context) -> doReturn("F").when(mock).getFName("u"));
				MockedConstruction<OrderServiceImpl> orderSvcConstruction = mockConstruction(OrderServiceImpl.class,
						(mock, context) -> doReturn(true).when(mock).addTransaction(any()));
				MockedStatic<MailMessage> mailMessage = mockStatic(MailMessage.class)) {

			OrderServiceImpl sut = new OrderServiceImpl();
			OrderServiceImpl spy = org.mockito.Mockito.spy(sut);
			ArgumentCaptor<OrderBean> orderCaptor = ArgumentCaptor.forClass(OrderBean.class);
			doReturn(true).when(spy).addOrder(orderCaptor.capture());

			String status = spy.paymentSuccess("u", 10.0);
			assertEquals("Order Placed Successfully!", status);

			OrderBean order = orderCaptor.getValue();
			assertEquals("p1", order.getProductId());
			assertEquals(2, order.getQuantity());
			assertEquals(100.0, order.getAmount(), 0.0001);

			verify(cartSvcConstruction.constructed().get(cartSvcConstruction.constructed().size() - 1), times(1))
					.removeAProduct("u", "p1");
			verify(prodSvcConstruction.constructed().get(prodSvcConstruction.constructed().size() - 1), times(1))
					.sellNProduct("p1", 2);
			verify(orderSvcConstruction.constructed().get(orderSvcConstruction.constructed().size() - 1), times(1))
					.addTransaction(any());
			mailMessage.verify(() -> MailMessage.transactionSuccess(org.mockito.Mockito.eq("u"), org.mockito.Mockito.eq("F"),
					any(String.class), org.mockito.Mockito.eq(10.0)), times(1));
			assertEquals(1, userSvcConstruction.constructed().size());
		}
	}

	@Test
	public void paymentSuccess_addOrderFails_stopsAndReturnsFailure() {
		CartBean item = new CartBean("u", "p1", 2);

		try (MockedConstruction<CartServiceImpl> cartSvcConstruction = mockConstruction(CartServiceImpl.class,
				(mock, context) -> doReturn(Arrays.asList(item)).when(mock).getAllCartItems("u"));
				MockedConstruction<ProductServiceImpl> prodSvcConstruction = mockConstruction(ProductServiceImpl.class,
						(mock, context) -> doReturn(5.0).when(mock).getProductPrice("p1"));
				MockedStatic<MailMessage> mailMessage = mockStatic(MailMessage.class)) {

			OrderServiceImpl sut = org.mockito.Mockito.spy(new OrderServiceImpl());
			doReturn(false).when(sut).addOrder(any());

			String status = sut.paymentSuccess("u", 10.0);
			assertEquals("Order Placement Failed!", status);

			verify(cartSvcConstruction.constructed().get(0), never()).removeAProduct(any(), any());
			assertEquals(1, prodSvcConstruction.constructed().size());
			verify(prodSvcConstruction.constructed().get(0), never()).sellNProduct(any(), any(Integer.class));
			mailMessage.verifyNoInteractions();
		}
	}

	@Test
	public void paymentSuccess_removeFromCartFails_stopsAndReturnsFailure() {
		CartBean item = new CartBean("u", "p1", 2);

		try (MockedConstruction<CartServiceImpl> cartSvcConstruction = mockConstruction(CartServiceImpl.class,
				(mock, context) -> {
					doReturn(Arrays.asList(item)).when(mock).getAllCartItems("u");
					doReturn(false).when(mock).removeAProduct("u", "p1");
				});
				MockedConstruction<ProductServiceImpl> prodSvcConstruction = mockConstruction(ProductServiceImpl.class,
						(mock, context) -> doReturn(5.0).when(mock).getProductPrice("p1"));
				MockedStatic<MailMessage> mailMessage = mockStatic(MailMessage.class)) {

			OrderServiceImpl sut = org.mockito.Mockito.spy(new OrderServiceImpl());
			doReturn(true).when(sut).addOrder(any());

			String status = sut.paymentSuccess("u", 10.0);
			assertEquals("Order Placement Failed!", status);

			verify(cartSvcConstruction.constructed().get(cartSvcConstruction.constructed().size() - 1), times(1))
					.removeAProduct("u", "p1");
			verify(prodSvcConstruction.constructed().get(prodSvcConstruction.constructed().size() - 1), never())
					.sellNProduct(any(), any(Integer.class));
			mailMessage.verifyNoInteractions();
		}
	}

	@Test
	public void paymentSuccess_sellNProductFails_stopsAndReturnsFailure() {
		CartBean item = new CartBean("u", "p1", 2);

		try (MockedConstruction<CartServiceImpl> cartSvcConstruction = mockConstruction(CartServiceImpl.class,
				(mock, context) -> {
					doReturn(Arrays.asList(item)).when(mock).getAllCartItems("u");
					doReturn(true).when(mock).removeAProduct("u", "p1");
				});
				MockedConstruction<ProductServiceImpl> prodSvcConstruction = mockConstruction(ProductServiceImpl.class,
						(mock, context) -> {
							doReturn(5.0).when(mock).getProductPrice("p1");
							doReturn(false).when(mock).sellNProduct("p1", 2);
						});
				MockedStatic<MailMessage> mailMessage = mockStatic(MailMessage.class)) {

			OrderServiceImpl sut = org.mockito.Mockito.spy(new OrderServiceImpl());
			doReturn(true).when(sut).addOrder(any());

			String status = sut.paymentSuccess("u", 10.0);
			assertEquals("Order Placement Failed!", status);

			verify(prodSvcConstruction.constructed().get(prodSvcConstruction.constructed().size() - 1), times(1))
					.sellNProduct("p1", 2);
			mailMessage.verifyNoInteractions();
		}
	}
}
