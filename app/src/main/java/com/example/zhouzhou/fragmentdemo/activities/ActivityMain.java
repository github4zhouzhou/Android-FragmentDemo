package com.example.zhouzhou.fragmentdemo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zhouzhou.fragmentdemo.R;
import com.example.zhouzhou.fragmentdemo.fragments.Fragment1;
import com.example.zhouzhou.fragmentdemo.fragments.Fragment2;
import com.example.zhouzhou.fragmentdemo.fragments.Fragment3;
import com.example.zhouzhou.fragmentdemo.fragments.Fragment4;
import com.example.zhouzhou.fragmentdemo.fragments.Fragment5;
import com.example.zhouzhou.fragmentdemo.fragments.FragmentBase;


public class ActivityMain extends ActivityBase implements FragmentBase.FragmentCallback {

    private int iSwitcher;
    private final int iStaticLoad = 2;
    private final int iDynamicLoad = 3;
    private final int iAddRemoveReplace = 4;
    private final int iBackStack = 5;
    private final int iBackStackRule = 6;
    private final int iShowHide = 7;
    private final int iAddReplaceBug = 8;
    private final int iArguments = 9;
    private boolean bSingleFragment = true;
    private int stackID1, stackID2, stackID3, stackID4;
    FragmentManager.OnBackStackChangedListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        iSwitcher = iStaticLoad;

        switch (iSwitcher) {
            case iStaticLoad:
                staticLoadFragment();
                break;
            case iAddRemoveReplace:
                arrFragment();
                break;
            case iBackStack:
                backStackFragment();
                break;
            case iShowHide:
                showOrHideFragment();
                break;
            case iAddReplaceBug:
                addReplaceFragmentBug();
                break;
            case iArguments:
                fragmentArguments(bSingleFragment);
                break;
            default:
                staticLoadFragment();
                break;
        }
    }

    private void staticLoadFragment() {
        setContentView(R.layout.activity_fragment_static_load);
    }


    // MARK: begin--------------------------------------------------------------------------
    /**
     * add, remove, replace fragment
     */
    private void arrFragment() {
        setContentView(R.layout.activity_fragment_add_remove_replace);

        Button btnAddFragment1 = (Button) findViewById(R.id.id_btn_add_frag1);
        Button btnAddFragment2 = (Button) findViewById(R.id.id_btn_add_frag2);
        Button btnRemoveFragment2 = (Button) findViewById(R.id.id_btn_remove_frag2);
        Button btnReplaceFragment1 = (Button) findViewById(R.id.id_btn_replace_frag1);

        btnAddFragment1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addFragment(new Fragment1(), "fragment1");
            }
        });

        btnAddFragment2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addFragment(new Fragment2(), "fragment2");
            }
        });

        btnRemoveFragment2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager manager = getSupportFragmentManager();
                Fragment fragment = manager.findFragmentByTag("fragment2");
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.remove(fragment);
                transaction.commit();
            }
        });

        btnReplaceFragment1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager manager = getSupportFragmentManager();
                Fragment2 fragment2 = new Fragment2();
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.replace(R.id.id_fragment_container, fragment2);
                transaction.commit();
            }
        });
    }
    // MARK: end--------------------------------------------------------------------------


    // MARK: begin--------------------------------------------------------------------------
    private void backStackFragment() {
        setContentView(R.layout.activity_fragment_back_stack);

        Button btnAddFragment1 = (Button) findViewById(R.id.id_btn_add_frag1);
        Button btnAddFragment2 = (Button) findViewById(R.id.id_btn_add_frag2);
        Button btnAddFragment3 = (Button) findViewById(R.id.id_btn_add_frag3);
        Button btnAddFragment4 = (Button) findViewById(R.id.id_btn_add_frag4);
        Button btnPopBackStack = (Button) findViewById(R.id.id_btn_pop_back_stack);
        Button btnBackToFrag2Param0 = (Button) findViewById(R.id.id_btn_back_to_frg2_0);
        Button btnBackToFrag2ParamInclusive = (Button) findViewById(R.id.id_btn_back_to_frg2_inclusive);

        btnAddFragment1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stackID1 = addFragmentAndBackStack(new Fragment1(), "fragment1");
            }
        });

        btnAddFragment2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stackID2 = addFragmentAndBackStack(new Fragment2(), "fragment2");
            }
        });

        btnAddFragment3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stackID3 = addFragmentAndBackStack(new Fragment3(), "fragment3");
            }
        });

        btnAddFragment4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stackID4 = addFragmentAndBackStack(new Fragment4(), "fragment4");
            }
        });

        btnPopBackStack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager manager = getSupportFragmentManager();
                manager.popBackStack();
            }
        });

        btnBackToFrag2Param0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager manager = getSupportFragmentManager();
                manager.popBackStack("fragment2", 0);  // 方法一,通过TAG回退
                // manager.popBackStack(stackID2, 0); // 方法二,通过Transaction ID回退
            }
        });

        btnBackToFrag2ParamInclusive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager manager = getSupportFragmentManager();
                // manager.popBackStack("fragment2",
                // FragmentManager.POP_BACK_STACK_INCLUSIVE);
                manager.popBackStack(stackID2, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });

        addOnBackStackChangedListener();
    }

    // MARK: end--------------------------------------------------------------------------


    // MARK: begin--------------------------------------------------------------------------
    private void showOrHideFragment() {
        setContentView(R.layout.activity_fragment_show_hide);

        Button btnAddFrag1 = (Button) findViewById(R.id.id_btn_add_frg1);
        Button btnAddFrag2 = (Button) findViewById(R.id.id_btn_add_frg2);
        Button btnAddFrag3 = (Button) findViewById(R.id.id_btn_add_frg3);
        Button btnHideFrag3 = (Button) findViewById(R.id.id_btn_frg3_hide);
        Button btnShowFrag3 = (Button) findViewById(R.id.id_btn_frg3_show);
        Button btnHideFrag2 = (Button) findViewById(R.id.id_btn_frg2_hide);
        Button btnShowFrag2 = (Button) findViewById(R.id.id_btn_frg2_show);
        Button btnDetachFrag3 = (Button) findViewById(R.id.id_btn_frg3_detach);
        Button btnAttachFrag3 = (Button) findViewById(R.id.id_btn_frg3_attach);
        Button btnDetachFrag2 = (Button) findViewById(R.id.id_btn_frg2_detach);
        Button btnAttachFrag2 = (Button) findViewById(R.id.id_btn_frg2_attach);
        Button btnFrag3IsAdded = (Button) findViewById(R.id.id_btn_frg3_is_added);

        // add fragment1
        btnAddFrag1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFragmentAndBackStack(new Fragment1(), "fragment1");
            }
        });

        // add fragment2
        btnAddFrag2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFragmentAndBackStack(new Fragment2(), "fragment2");
            }
        });

        // add fragment3
        btnAddFrag3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFragmentAndBackStack(new Fragment3(), "fragment3");
            }
        });

        // hide fragment3
        btnHideFrag3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFragment(false, "fragment3");
            }
        });

        // show fragment3
        btnShowFrag3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFragment(true, "fragment3");
            }
        });

        // hide fragment2
        btnHideFrag2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFragment(false, "fragment2");
            }
        });

        // show fragment2
        btnShowFrag2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFragment(true, "fragment2");
            }
        });

        // detach fragment3
        btnDetachFrag3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attachFragment(false, "fragment3");
            }
        });

        // attach fragment3
        btnAttachFrag3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attachFragment(true, "fragment3");
            }
        });

        // detach fragment2
        btnDetachFrag2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attachFragment(false, "fragment2");
            }
        });

        // attach fragment2
        btnAttachFrag2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attachFragment(true, "fragment2");
            }
        });

        // fragment3 is added
        btnFrag3IsAdded.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getSupportFragmentManager();
                Fragment fragment = manager.findFragmentByTag("fragment3");
                Toast.makeText(ActivityMain.this
                        , "attached, fragment is added: " + fragment.isAdded()
                        , Toast.LENGTH_LONG).show();
            }
        });
    }

    // MARK: begin--------------------------------------------------------------------------
    /**
     * 这个演示已经没有必要了，因为新的版本已经修复了这个bug
     * 原来版本（具体哪个版本不记得了），replace 函数是清空container中所有的fragment实例，
     * 然后再将指定的fragment添加到container的ADD队列中；
     * 现在版本相当于 add 函数了
     */
    private void addReplaceFragmentBug() {
        setContentView(R.layout.activity_fragment_add_replace_bug);

        Button btnAddFrag1 = (Button) findViewById(R.id.id_btn_add_frg1);
        Button btnAddFrag2 = (Button) findViewById(R.id.id_btn_add_frg2);
        Button btnAddFrag3 = (Button) findViewById(R.id.id_btn_add_frg3);
        Button btnAddFrag4 = (Button) findViewById(R.id.id_btn_add_frg4);
        Button btnAddFrag5 = (Button) findViewById(R.id.id_btn_add_frg5);
        Button btnReplaceFrag5 = (Button) findViewById(R.id.id_btn_replace_frg5);
        Button btnPrintStack = (Button) findViewById(R.id.id_btn_print_stack);

        btnAddFrag1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFragmentAndBackStack(new Fragment1(), "add fragment1");
            }
        });

        btnAddFrag2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFragmentAndBackStack(new Fragment2(), "add fragment2");
            }
        });

        btnAddFrag3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFragmentAndBackStack(new Fragment3(), "add fragment3");
            }
        });

        btnAddFrag4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFragmentAndBackStack(new Fragment4(), "add fragment4");
            }
        });

        btnAddFrag5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFragmentAndBackStack(new Fragment5(), "add fragment5");
            }
        });

        btnReplaceFrag5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment5 fragment5 = new Fragment5();
                FragmentManager manager = getSupportFragmentManager();
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.replace(R.id.id_fragment_container, fragment5);
                transaction.addToBackStack("replace fragment5");
                transaction.commit();
            }
        });

        btnPrintStack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv = (TextView) findViewById(R.id.id_tv_stack_val);

                FragmentManager manager = getSupportFragmentManager();
                int count = manager.getBackStackEntryCount();
                StringBuilder builder = new StringBuilder("回退栈内容为:\n");
                for (int i = --count; i >= 0; i--) {
                    FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(i);
                    builder.append(entry.getName() + "\n");
                }
                tv.setText(builder.toString());
            }
        });
    }

    // MARK: end--------------------------------------------------------------------------


    // MARK: begin--------------------------------------------------------------------------

    private void fragmentArguments(boolean bSingleFragment) {
        setContentView(R.layout.activity_fragment_arguments);

        if (bSingleFragment) {
            View view = findViewById(R.id.id_fragment4);
            view.setVisibility(View.GONE);
        }
    }

    // MARK: end--------------------------------------------------------------------------


    // MARK: begin--------------------------------------------------------------------------

    private void addOnBackStackChangedListener() {
        final FragmentManager manager = getSupportFragmentManager();

        /**
         * 即使在 onCreate 中调用此函数也要判断 null，
         * 因为当Activity因为配置发生改变（屏幕旋转）或者内存不足被系统杀死，
         * 造成重新创建时，我们的fragment会被保存下来，但是会创建新的FragmentManager，
         * 新的FragmentManager会首先会去获取保存下来的fragment队列，重建fragment队列，从而恢复之前的状态。
         */
        if (listener == null) {
            listener = new FragmentManager.OnBackStackChangedListener() {
                @Override
                public void onBackStackChanged() {
                    int count = manager.getBackStackEntryCount();
                    Log.d("ActivityMain", "back stack changed, count = " + count);
                    if (count > 0) {
                        FragmentManager.BackStackEntry bsEntry = manager.getBackStackEntryAt(count - 1);
                        Log.d("ActivityMain", "entry name = " + bsEntry.getName());
                        Log.d("ActivityMain", "entry id = " + bsEntry.getId());
                    }
                }
            };
            manager.addOnBackStackChangedListener(listener);
        }
    }

    private void attachFragment(boolean bAttach, String tagName) {
        FragmentManager manager = getSupportFragmentManager();
        Fragment fragment = manager.findFragmentByTag(tagName);
        FragmentTransaction transaction = manager.beginTransaction();
        if (bAttach) {
            transaction.attach(fragment);
            transaction.addToBackStack("attach " + tagName);
        } else {
            transaction.detach(fragment);
            transaction.addToBackStack("detach " + tagName);
        }
        transaction.commit();
    }

    private void showFragment(boolean bShow, String tagName) {
        FragmentManager manager = getSupportFragmentManager();
        Fragment fragment = manager.findFragmentByTag(tagName);
        FragmentTransaction transaction = manager.beginTransaction();
        if (bShow) {
            transaction.show(fragment);
            transaction.addToBackStack("show " + tagName);
        } else {
            transaction.hide(fragment);
            transaction.addToBackStack("hide" + tagName);
        }
        transaction.commit();
    }

    private int addFragment(Fragment fragment, String tagName) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(R.id.id_fragment_container, fragment, tagName);
        return transaction.commit();
    }

    private int addFragmentAndBackStack(Fragment fragment, String tagName) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(R.id.id_fragment_container, fragment, tagName);
        transaction.addToBackStack(tagName);
        return transaction.commit();
    }

    private void addOtherFragments() {
        Fragment2 fragment2 = new Fragment2();
        Fragment3 fragment3 = new Fragment3();
        Fragment4 fragment4 = new Fragment4();
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(R.id.id_fragment_container, fragment2);
        transaction.add(R.id.id_fragment_container, fragment3);
        transaction.add(R.id.id_fragment_container, fragment4);
        transaction.addToBackStack("other fragments");
        transaction.commit();
    }

    // MARK: end--------------------------------------------------------------------------

    @Override
    public void dealWithString(String str) {
        if (bSingleFragment) {
            Intent intent = new Intent(ActivityMain.this, ActivityOther.class);
            intent.putExtra("title", str);
            startActivity(intent);
        } else {
            FragmentManager manager = getSupportFragmentManager();
            Fragment4 fragment4 = (Fragment4)manager.findFragmentById(R.id.id_fragment4);
            fragment4.setText(str);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg.what == 1) {
            String str = (String)msg.obj;
            dealWithString(str);
        }
    }

    @Override
    protected void onDestroy() {
        if (listener != null) {
            FragmentManager manager = getSupportFragmentManager();
            manager.removeOnBackStackChangedListener(listener);
        }
        super.onDestroy();
    }
}
