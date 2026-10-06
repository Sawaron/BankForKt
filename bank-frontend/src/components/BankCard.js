import React, { useState } from 'react';
import { useSelector } from 'react-redux';
import './BankCard.css';

const BankCard = () => {
    // Забираем живые данные из Redux-хранилища
    const { cardNumber, ownerName, balance } = useSelector((state) => state.account);
    // Состояние для скрытия/показа баланса (глазок)
    const [showBalance, setShowBalance] = useState(true);

    return (
        <div className="card-container">
            <div className="bank-card">
                {/* Верхняя часть карты: Лого и Чип */}
                <div className="card-header">
                    <span className="bank-logo">SaCQ BANK</span>
                    <div className="card-chip"></div>
                </div>

                {/* Баланс посередине */}
                <div className="card-balance-section">
                    <p className="balance-label">Текущий баланс</p>
                    <div className="balance-row">
                        <h1 className="card-balance">
                            {showBalance ? `${Number(balance).toFixed(2)} ₽` : '••••••'}
                        </h1>
                        <button
                            className="eye-button"
                            onClick={() => setShowBalance(!showBalance)}
                        >
                            {showBalance ? '👁️' : '🙈'}
                        </button>
                    </div>
                </div>

                {/* Нижняя часть: Номер и Владелец */}
                <div className="card-footer">
                    <div className="card-number-display">
                        {cardNumber || '00-00-0000-0000-0000'}
                    </div>
                    <div className="card-holder">
                        <span className="holder-label">CARDHOLDER</span>
                        <p className="holder-name">{ownerName ? ownerName.toUpperCase() : 'YOUR NAME'}</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default BankCard;