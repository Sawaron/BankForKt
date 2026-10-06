import React, { useState } from 'react';
import { useDispatch } from 'react-redux';
import axios from 'axios';
import { loginSuccess } from '../redux/accountSlice';
import './AuthPage.css';

const AuthPage = () => {
    const dispatch = useDispatch();
    const [isLoginTab, setIsLoginTab] = useState(true);

    // Поля для формы
    const [ownerName, setOwnerName] = useState('');
    const [password, setPassword] = useState('');
    const [cardNumber, setCardNumber] = useState('');

    // Для вывода ошибок и красивых сообщений
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    // Базовый URL твоего Spring Boot бэкенда
    const API_URL = 'http://localhost:8080/account';

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccessMessage('');

        try {
            if (isLoginTab) {
                // ---- ЛОГИКА ВХОДА ----
                const response = await axios.get(`${API_URL}/balance`, {
                    params: { cardNumber, password }
                });

// Теперь мы берём реальное имя и баланс, которые прислал Spring Boot!
                dispatch(loginSuccess({
                    cardNumber: cardNumber,
                    ownerName: response.data.ownerName, // <-- Вот она, магия!
                    balance: response.data.balance      // <-- Берем чистый баланс
                }));
            } else {
                // ---- ЛОГИКА РЕГИСТРАЦИИ (Создание карты) ----
                // На бэкенде у тебя метод saveAccount(pin, owner) или createAccount через Request Body
                // Давай отправим POST-запрос на создание
                const response = await axios.post(`${API_URL}/create`, {
                    ownerName,
                    password
                });

                // Бэкенд возвращает созданный BankAccountResponse с номером карты
                const newCard = response.data;

                setSuccessMessage(`Карта успешно создана! Ваш номер: ${newCard.cardNumber}`);
                // Сразу же логиним пользователя с его новой картой
                dispatch(loginSuccess({
                    cardNumber: newCard.cardNumber,
                    ownerName: newCard.ownerName,
                    balance: newCard.balance
                }));
            }
        } catch (err) {
            // Если бэкенд выбросил BankTerminalException, Axios поймает его тут
            const serverMessage = err.response?.data?.message || 'Ошибка связи с сервером терминала';
            setError(serverMessage);
        }
    };

    return (
        <div className="auth-container">
            <div className="tabs">
                <button
                    className={`tab-btn ${isLoginTab ? 'active' : ''}`}
                    onClick={() => { setIsLoginTab(true); setError(''); }}
                >
                    Вставить карту
                </button>
                <button
                    className={`tab-btn ${!isLoginTab ? 'active' : ''}`}
                    onClick={() => { setIsLoginTab(false); setError(''); }}
                >
                    Выпустить карту
                </button>
            </div>

            <form onSubmit={handleSubmit} className="auth-form">
                {!isLoginTab && (
                    <div className="input-group">
                        <label>Имя владельца</label>
                        <input
                            type="text"
                            placeholder="Например, SHOMA"
                            value={ownerName}
                            onChange={(e) => setOwnerName(e.target.value)}
                            required
                        />
                    </div>
                )}

                {isLoginTab && (
                    <div className="input-group">
                        <label>Номер карты</label>
                        <input
                            type="text"
                            placeholder="XX-XX-XXXX-XXXX-XXXX"
                            value={cardNumber}
                            onChange={(e) => setCardNumber(e.target.value)}
                            required
                        />
                    </div>
                )}

                <div className="input-group">
                    <label>ПИН-код</label>
                    <input
                        type="password"
                        maxLength="20"
                        placeholder="••••"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                </div>

                {error && <div className="error-message">❌ {error}</div>}
                {successMessage && <div className="success-message">🎉 {successMessage}</div>}

                <button type="submit" className="submit-btn">
                    {isLoginTab ? 'Войти в систему' : 'Создать аккаунт'}
                </button>
            </form>
        </div>
    );
};

export default AuthPage;