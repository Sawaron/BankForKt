import { createSlice } from '@reduxjs/toolkit';

const savedAccount = JSON.parse(localStorage.getItem('bank_account'));

const initialState = savedAccount || {
    isLoggedIn: false,
    cardNumber: '',
    ownerName: '',
    balance: 0
};

const accountSlice = createSlice({
    name: 'account',
    initialState,
    reducers: {
        loginSuccess: (state, action) => {
            state.isLoggedIn = true;
            state.cardNumber = action.payload.cardNumber;
            state.ownerName = action.payload.ownerName;
            state.balance = action.payload.balance;
            localStorage.setItem('bank_account', JSON.stringify(state));
        },
        updateBalance: (state, action) => {
            state.balance = action.payload;
            localStorage.setItem('bank_account', JSON.stringify(state));
        },
        logout: (state) => {
            state.isLoggedIn = false;
            state.cardNumber = '';
            state.ownerName = '';
            state.balance = 0;
            localStorage.removeItem('bank_account');
        }
    }
});

export const { loginSuccess, updateBalance, logout } = accountSlice.actions;
export default accountSlice.reducer;