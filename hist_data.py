import requests
import json
import pandas as pd
from datetime import datetime
import time

def scrape_historical_data_from_yahoo_finance(symbol, state_date, end_date):
    url = f"https://query2.finance.yahoo.com/v8/finance/chart/{symbol}?formatted=true&crumb=5r2%2FsuPmrJd&lang=en-US&region=US&includeAdjustedClose=true&interval=1d&period1={state_date}&period2={end_date}&events=capitalGain%7Cdiv%7Csplit&useYfid=true&corsDomain=finance.yahoo.com"
    
    headers = {
        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36 Edg/118.0.2088.69",
        "authority": "query1.finance.yahoo.com",
        "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
        "accept-language": "en-US,en;q=0.9",
        "cache-control": "max-age=0"
    }

    response = requests.get(url, headers=headers)

    if response.status_code == 200:
        data = response.json()

        # Extract data
        result = data["chart"]["result"][0]
        timestamps = result["timestamp"]
        dates = [ datetime.utcfromtimestamp(t).strftime('%d/%m/%Y')for t in timestamps]
        quote = result["indicators"]["quote"][0]
        open_prices = [ round(q, 2) for q in quote["open"] ]
        high_prices = [ round(q, 2) for q in quote["high"] ]
        low_prices = [ round(q, 2) for q in quote["low"] ]
        close_prices = [ round(q, 2) for q in quote["close"] ]
        volume = quote["volume"]
        adjclose = [ round(q, 2) for q in data["chart"]["result"][0]["indicators"]["adjclose"][0]["adjclose"] ]

        # Create DataFrame
        df = pd.DataFrame({
            'Date': dates,
            'Open': open_prices,
            'High': high_prices,
            'Low': low_prices,
            'Close': close_prices,
            'Adj Close': adjclose,
            'Volume': volume
        })

        # Save DataFrame to CSV
        df.to_csv(f'{symbol}.csv', index=False)

        print("saved", symbol)

    else:
        print("Failed to fetch data:", symbol, response.status_code)

def main():
    # This script can be used to download historical data in CSV format from Yahoo Finance.
    # Update the symbols list with the stock prices you wish to download and they will be auto-saved
    # to the data folder in this project

    state_date = int(datetime.strptime("01/01/1990", '%d/%m/%Y').timestamp())
    end_date = int(datetime.strptime("18/09/2024", '%d/%m/%Y').timestamp())
    
    symbols = [
            'CROX',
            'CRAWA',
            'DAC',
            'SKX',
            'BLD',
            'LEN',
            'FIX',
            'DECK',
            'MCFT',
            'OLLI',
            'BOOT',
            'EACO',
            'PLUS',
            'SPNS',
            'CVCO',
            'ALRM',
            'COST',
            'UFPT',
            'FTLF',
            'NSSC',
        
    ]
    symbols = ['XLF']
    for symbol in symbols:
        scrape_historical_data_from_yahoo_finance(symbol, state_date, end_date)
        time.sleep(2)

if __name__ == "__main__":
    main()
