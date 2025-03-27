import requests
import json
import pandas as pd
from datetime import datetime
import time
import traceback


def scrape_historical_data_from_yahoo_finance(symbol, state_date, end_date):
    url = f"https://query2.finance.yahoo.com/v8/finance/chart/{symbol}?formatted=true&crumb=5r2%2FsuPmrJd&lang=en-US&region=US&includeAdjustedClose=true&interval=1d&period1={state_date}&period2={end_date}&events=capitalGain%7Cdiv%7Csplit&useYfid=true&corsDomain=finance.yahoo.com"

    headers = {
        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36 Edg/118.0.2088.69",
        "authority": "query1.finance.yahoo.com",
        "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
        "accept-language": "en-US,en;q=0.9",
        "cache-control": "max-age=0"
    }

    try:
        response = requests.get(url, headers=headers)
        response.raise_for_status()
        data = response.json()

        # Validate structure before continuing
        if (
            "chart" in data and
            "result" in data["chart"] and
            data["chart"]["result"] and
            "timestamp" in data["chart"]["result"][0]
        ):
            result = data["chart"]["result"][0]
            timestamps = result["timestamp"]
            dates = [datetime.utcfromtimestamp(t).strftime('%d/%m/%Y') for t in timestamps]
            quote = result["indicators"]["quote"][0]
            open_prices = [round(q, 2) if q is not None else None for q in quote["open"]]
            high_prices = [round(q, 2) if q is not None else None for q in quote["high"]]
            low_prices = [round(q, 2) if q is not None else None for q in quote["low"]]
            close_prices = [round(q, 2) if q is not None else None for q in quote["close"]]
            adjclose = [round(q, 2) if q is not None else None for q in result["indicators"]["adjclose"][0]["adjclose"]]
            volume = quote["volume"]

            df = pd.DataFrame({
                'Date': dates,
                'Open': open_prices,
                'High': high_prices,
                'Low': low_prices,
                'Close': close_prices,
                'Adj Close': adjclose,
                'Volume': volume
            })

            df.to_csv(f'all/{symbol}.csv', index=False)
            print("saved", symbol)
        else:
            print(f"Invalid structure for {symbol}")
            print(json.dumps(data, indent=2))  # Dump full JSON for debugging

    except Exception as e:
        print(f"Error while processing {symbol}: {e}")
        traceback.print_exc()
        try:
            print("Response text:")
            print(json.dumps(response.json(), indent=2))  # Try to dump JSON if available
        except Exception:
            print("Could not decode response as JSON.")
            print(response.text[:1000])  # Dump a chunk of raw response just in case


def main():
    # This script can be used to download historical data in CSV format from Yahoo Finance.
    # Update the symbols list with the stock prices you wish to download and they will be auto-saved
    # to the data folder in this project

    state_date = int(datetime.strptime("01/01/1990", '%d/%m/%Y').timestamp())
    end_date = int(datetime.strptime("18/09/2024", '%d/%m/%Y').timestamp())
    
    #symbols = [
    #        'CROX',
    #        'CRAWA',
    #        'DAC',
    #        'SKX',
    #        'BLD',
    #        'LEN',
    #        'FIX',
    #        'DECK',
    #        'MCFT',
    #        'OLLI',
    #        'BOOT',
    #        'EACO',
    #        'PLUS',
    #        'SPNS',
    #        'CVCO',
    #        'ALRM',
    #        'COST',
    #        'UFPT',
    #        'FTLF',
    #        'NSSC',
    #]
    #symbols = ['XLF']
    
    with open('universe.txt', 'r') as file:
        symbols = [line.strip() for line in file if line.strip()]
    
    for symbol in symbols:
        scrape_historical_data_from_yahoo_finance(symbol, state_date, end_date)
        time.sleep(2)

if __name__ == "__main__":
    main()
