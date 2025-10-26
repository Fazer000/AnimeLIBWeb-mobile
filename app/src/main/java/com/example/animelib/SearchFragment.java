package com.example.animelib;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.adapters.SearchResultsAdapter;
import com.example.animelib.api.ApiService;
import com.example.animelib.models.SearchResponse;
import com.google.android.material.tabs.TabLayout;

/**
 * Фрагмент быстрого поиска аниме
 */
public class SearchFragment extends Fragment {
    private static final String TAG = "SearchFragment";
    private static final long SEARCH_DELAY_MS = 500; // Задержка перед поиском
    
    private EditText searchInput;
    private ImageButton clearButton;
    private ImageButton closeButton;
    private TabLayout searchTabLayout;
    private RecyclerView searchResults;
    private LinearLayout emptyState;
    private View loadingIndicator;
    
    private SearchResultsAdapter adapter;
    private ApiService apiService;
    private Handler searchHandler;
    private Runnable searchRunnable;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        
        // Initialize API service
        apiService = new ApiService(requireContext());
        searchHandler = new Handler(Looper.getMainLooper());
        
        initializeViews(view);
        setupListeners();
        
        Log.d(TAG, "SearchFragment created");
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Обработка системной кнопки назад
        setupBackPressHandler();
        
        // Фокус на инпуте с открытием клавиатуры
        openKeyboard();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel pending search
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        // Закрываем клавиатуру при выходе
        closeKeyboard();
    }
    
    /**
     * Настраивает обработку системной кнопки назад
     */
    private void setupBackPressHandler() {
        requireActivity().getOnBackPressedDispatcher().addCallback(
            getViewLifecycleOwner(),
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    Log.d(TAG, "Back button pressed, closing search fragment");
                    // Закрываем клавиатуру
                    closeKeyboard();
                    // Закрываем фрагмент
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                }
            }
        );
    }
    
    /**
     * Открывает клавиатуру и устанавливает фокус на поле поиска
     */
    private void openKeyboard() {
        if (searchInput != null) {
            searchInput.requestFocus();
            searchInput.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
                    Log.d(TAG, "Keyboard opened");
                }
            }, 100); // Небольшая задержка для надежности
        }
    }
    
    /**
     * Закрывает клавиатуру
     */
    private void closeKeyboard() {
        if (searchInput != null && getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) requireActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
                Log.d(TAG, "Keyboard closed");
            }
        }
    }
    
    private void initializeViews(View view) {
        searchInput = view.findViewById(R.id.searchInput);
        clearButton = view.findViewById(R.id.clearButton);
        closeButton = view.findViewById(R.id.closeButton);
        searchTabLayout = view.findViewById(R.id.searchTabLayout);
        searchResults = view.findViewById(R.id.searchResults);
        emptyState = view.findViewById(R.id.emptyState);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        
        // Setup RecyclerView
        adapter = new SearchResultsAdapter();
        adapter.setOnItemClickListener(new SearchResultsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SearchResponse.AnimeSearchItem item) {
                onAnimeItemClick(item);
            }
            
            @Override
            public void onItemLongClick(SearchResponse.AnimeSearchItem item) {
                onAnimeItemLongClick(item);
            }
        });
        searchResults.setAdapter(adapter);
        searchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResults.setVisibility(View.GONE);
        
        // Setup TabLayout
        setupTabLayout();
    }
    
    private void setupTabLayout() {
        // Отключаем uppercase для текста табов
        for (int i = 0; i < searchTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = searchTabLayout.getTabAt(i);
            if (tab != null && tab.view != null) {
                android.widget.TextView textView = (android.widget.TextView) tab.view.getChildAt(1);
                if (textView != null) {
                    textView.setAllCaps(false);
                }
            }
        }
        
        searchTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                Log.d(TAG, "Tab selected: " + position);
                
                // В будущем здесь будет переключение между разными типами поиска
                // Пока что только "Тайтлы"
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }
    
    private void setupListeners() {
        // Close button
        closeButton.setOnClickListener(v -> {
            closeKeyboard();
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        
        // Clear button
        clearButton.setOnClickListener(v -> {
            searchInput.setText("");
            searchInput.requestFocus();
            adapter.clearItems();
            showEmptyState();
        });
        
        // Search input text watcher with debounce
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Show/hide clear button
                clearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                
                // Cancel previous search
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                
                // Perform search with delay
                if (s.length() > 0) {
                    String query = s.toString().trim();
                    searchRunnable = () -> performSearch(query);
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
                } else {
                    adapter.clearItems();
                    showEmptyState();
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        
        // Search action on keyboard
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    // Cancel debounce and search immediately
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    performSearch(query);
                }
                return true;
            }
            return false;
        });
    }
    
    private void performSearch(String query) {
        if (query.trim().isEmpty()) {
            showEmptyState();
            return;
        }
        
        Log.d(TAG, "Performing search for: " + query);
        
        // Show loading
        showLoading();
        
        // Perform API search
        apiService.searchAnime(query, new ApiService.SearchCallback() {
            @Override
            public void onSearchResults(SearchResponse response) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    if (response.getData() != null && !response.getData().isEmpty()) {
                        adapter.setItems(response.getData());
                        showResults();
                        Log.d(TAG, "Search completed: " + response.getData().size() + " results");
                    } else {
                        adapter.clearItems();
                        showEmptyState();
                        Log.d(TAG, "Search completed: no results");
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    Log.e(TAG, "Search error: " + error);
                    Toast.makeText(getContext(), "Ошибка поиска: " + error, Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
            }
        });
    }
    
    /**
     * Обычный клик - открывает страницу аниме в WebView
     */
    private void onAnimeItemClick(SearchResponse.AnimeSearchItem item) {
        Log.d(TAG, "Anime clicked: " + item.getRusName() + " (slug_url: " + item.getSlugUrl() + ")");
        
        // Закрываем клавиатуру
        closeKeyboard();
        
        // Формируем URL для WebView: /ru/anime/{slug_url}
        String webViewUrl = "/ru/anime/" + item.getSlugUrl();
        
        // Закрываем фрагмент поиска
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
            
            // Открываем URL в WebView MainActivity
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadUrlInWebView(webViewUrl);
            }
        }
    }
    
    /**
     * Долгий клик - открывает VideoPlayerActivity
     */
    private void onAnimeItemLongClick(SearchResponse.AnimeSearchItem item) {
        Log.d(TAG, "Anime long clicked: " + item.getRusName() + " (slug_url: " + item.getSlugUrl() + ")");
        
        // Закрываем клавиатуру
        closeKeyboard();
        
        // Open VideoPlayerActivity with anime URL
        String animeUrl = "https://api.cdnlibs.org/api/anime/" + item.getSlugUrl();
        
        Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
        intent.putExtra("anime_url", animeUrl);
        startActivity(intent);
        
        // Закрываем фрагмент поиска
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }
    
    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        searchResults.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.GONE);
    }
    
    private void showLoading() {
        emptyState.setVisibility(View.GONE);
        searchResults.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.VISIBLE);
    }
    
    private void showResults() {
        emptyState.setVisibility(View.GONE);
        searchResults.setVisibility(View.VISIBLE);
        loadingIndicator.setVisibility(View.GONE);
    }
}

