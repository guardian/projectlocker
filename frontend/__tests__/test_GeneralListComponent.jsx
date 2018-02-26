import React from 'react';
import {shallow,mount} from 'enzyme';
import moxios from 'moxios';
import GeneralListComponent from '../app/GeneralListComponent.jsx';
import sinon from 'sinon';

describe("GeneralListComponent", ()=> {
    beforeEach(() => moxios.install());
    afterEach(() => moxios.uninstall());

    test("standardColumn should return a column definition", ()=>{
        const result = GeneralListComponent.standardColumn("ColumnName","data_key");
        expect(result.header).toEqual("ColumnName");
        expect(result.key).toEqual("data_key");
        expect(result.headerProps.className).toEqual("dashboardheader");
    });

    test("should render a SortableTable", (done)=>{
        const result = shallow(<GeneralListComponent title="test title"/>);

        const rendered_table = result.find('SortableTable');
        expect(rendered_table).toBeTruthy();
        expect(rendered_table.props().data).toEqual([]);
        expect(rendered_table.props().columns).toEqual([]);
        expect(rendered_table.props().style).toEqual(result.instance().style);
        expect(rendered_table.props().iconStyle).toEqual(result.instance().iconStyle);

        const title = result.find('h2.list-title');
        expect(title.text()).toEqual("test title");
        done();
    });

    test("getNextPage should download data and then call gotDataCallback", (done)=>{
        const result = shallow(<GeneralListComponent title="test title"/>);
        const callbackSpy = sinon.spy();
        result.instance().gotDataCallback = callbackSpy;
        result.instance().endpoint = "/path/to/fake/endpoint";

        result.instance().getNextPage();

        return moxios.wait(()=>{
            expect(moxios.requests.mostRecent().config.url).toEqual('/path/to/fake/endpoint?startAt=0&length=20');
            let request = moxios.requests.mostRecent();
            request.respondWith({
                status: 200,
                response: {'status': 'ok','data': 'test'}
            }).then(() => {
                expect(callbackSpy.calledOnce).toBeTruthy();
                done();
            }).catch(error=>{
                console.error(error);
                done.fail(error);
            })
        });
    });
});
