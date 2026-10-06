import React from 'react';
import { useSelector } from 'react-redux';
import BankCard from './components/BankCard';
import AuthPage from './pages/AuthPage';
import OperationsPanel from './components/OperationsPanel';

function App() {
    const isLoggedIn = useSelector((state) => state.account.isLoggedIn);

    return (
        <div style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            minHeight: '100vh',
            background: '#0f172a',
            padding: '20px'
        }}>

            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '20px' }}>
                <BankCard />
                {!isLoggedIn ? <AuthPage /> : <OperationsPanel />}
            </div>

        </div>
    );
}

export default App;