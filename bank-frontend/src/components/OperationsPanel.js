import React, { useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import axios from 'axios';
import { updateBalance, logout } from '../redux/accountSlice';
import './OperationsPanel.css';

const OperationsPanel = () => {
    const dispatch = useDispatch();
    const { cardNumber } = useSelector((state) => state.account);

    // Вкладки: 'deposit' (пополнение), 'withdraw' (снятие), 'transfer' (перевод) или null
    const [activeTab, setActiveTab] = useState(null);
    const [amount, setAmount] = useState('');
    const [password, setPassword] = useState('');
    const [cardNumberTo, setCardNumberTo] = useState('');

    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const API_URL = 'http://localhost:8080/account';

    const switchTab = (tabName) => {
        setActiveTab(activeTab === tabName ? null : tabName);
        setAmount('');
        setPassword('');
        setCardNumberTo('');
        setError('');
        setSuccess('');
    };

    const handleOperation = async (e, type) => {
        e.preventDefault();
        setError('');
        setSuccess('');

        try {
            if (type === 'deposit') {
                await axios.put(`${API_URL}/deposit`, { cardNumber, password, amount: parseFloat(amount) });
                setSuccess('Баланс успешно пополнен!');
            } else if (type === 'withdraw') {
                await axios.put(`${API_URL}/withdraw`, { cardNumber, password, amount: parseFloat(amount) });
                setSuccess('Деньги успешно сняты!');
            } else if (type === 'transfer') {
                await axios.put(`${API_URL}/transfer`, { cardNumberFrom: cardNumber, password, cardNumberTo, amount: parseFloat(amount) });
                setSuccess('Перевод успешно выполнен!');
            }

            // Синхронизируем баланс в приложении, запрашивая актуальные данные с бэкенда
            const balanceResponse = await axios.get(`${API_URL}/balance`, { params: { cardNumber, password } });
            dispatch(updateBalance(balanceResponse.data.balance));

            // Очищаем поля ввода после успешной операции
            setAmount('');
            setPassword('');
            setCardNumberTo('');
        } catch (err) {
            const serverMessage = err.response?.data?.message || 'Произошла ошибка при выполнении операции';
            setError(serverMessage);
        }
    };

    return (
        <div className="operations-container">
            {/* Твой классический ряд кнопок управления банкоматом */}
            <div className="buttons-row">
                <button className={`op-btn ${activeTab === 'deposit' ? 'active' : ''}`} onClick={() => switchTab('deposit')}>
                    💵 Пополнить
                </button>
                <button className={`op-btn ${activeTab === 'withdraw' ? 'active' : ''}`} onClick={() => switchTab('withdraw')}>
                    🏪 Снять
                </button>
                <button className={`op-btn ${activeTab === 'transfer' ? 'active' : ''}`} onClick={() => switchTab('transfer')}>
                    🔄 Перевести
                </button>
            </div>

            {error && <div className="op-error">❌ {error}</div>}
            {success && <div className="op-success">✅ {success}</div>}

            {/* 1. ФОРМА ПОПОЛНЕНИЯ БАЛАНСА */}
            {activeTab === 'deposit' && (
                <form onSubmit={(e) => handleOperation(e, 'deposit')} className="op-form">
                    <h3>Пополнение счета</h3>
                    <input type="number" placeholder="Сумма пополнения" value={amount} onChange={(e) => setAmount(e.target.value)} required min="1" step="0.01"/>
                    <input type="password" placeholder="Ваш ПИН-код для подтверждения" maxLength="20" value={password} onChange={(e) => setPassword(e.target.value)} required />
                    <button type="submit" className="action-submit-btn deposit-bg">Пополнить баланс</button>
                </form>
            )}

            {/* 2. ФОРМА СНЯТИЯ НАЛИЧНЫХ */}
            {activeTab === 'withdraw' && (
                <form onSubmit={(e) => handleOperation(e, 'withdraw')} className="op-form">
                    <h3>Снятие наличных</h3>
                    <input type="number" placeholder="Сумма снятия" value={amount} onChange={(e) => setAmount(e.target.value)} required min="1" step="0.01"/>
                    <input type="password" placeholder="Ваш ПИН-код для подтверждения" maxLength="20" value={password} onChange={(e) => setPassword(e.target.value)} required />
                    <button type="submit" className="action-submit-btn withdraw-bg">Получить наличные</button>
                </form>
            )}

            {/* 3. ФОРМА ПЕРЕВОДА НА ДРУГУЮ КАРТУ */}
            {activeTab === 'transfer' && (
                <form onSubmit={(e) => handleOperation(e, 'transfer')} className="op-form">
                    <h3>Перевод на другую карту</h3>
                    <input type="text" placeholder="Номер карты получателя (XX-XX-...)" value={cardNumberTo} onChange={(e) => setCardNumberTo(e.target.value)} required />
                    <input type="number" placeholder="Сумма перевода" value={amount} onChange={(e) => setAmount(e.target.value)} required min="1" step="0.01"/>
                    <input type="password" placeholder="Ваш ПИН-код для подтверждения" maxLength="20" value={password} onChange={(e) => setPassword(e.target.value)} required />
                    <button type="submit" className="action-submit-btn transfer-bg">Отправить перевод</button>
                </form>
            )}

            {/* Кнопка выхода */}
            <button className="logout-btn" onClick={() => dispatch(logout())}>
                🚪 Выйти из системы
            </button>
        </div>
    );
};

export default OperationsPanel;