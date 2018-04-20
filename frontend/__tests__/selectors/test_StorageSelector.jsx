import React from 'react';
import {shallow,mount} from 'enzyme';
import StorageSelector from '../../app/Selectors/StorageSelector.jsx';
import sinon from 'sinon';
import assert from 'assert';

describe("StorageSelector", ()=>{
    it("should present the provided options in a combo box", ()=>{
        const updateCb = sinon.spy();
        const storages = [
            {id:1, storageType: "Local", rootpath: "/path1"},
            {id:2, storageType: "Local", rootpath: "/path2"},

        ];
        const rendered = shallow(<StorageSelector enabled={true} selectedStorage={1} selectionUpdated={updateCb} storageList={storages}/>);

        const options = rendered.find('option');
        expect(options.length).toEqual(2);
        expect(options.at(0).props().value).toEqual(1);
        expect(options.at(0).text()).toEqual("/path1 on Local");
        expect(options.at(1).props().value).toEqual(2);
        expect(options.at(1).text()).toEqual("/path2 on Local");
        assert(updateCb.notCalled)
    });

    it("should call the provided update function when the selection is changed", ()=>{
        const updateCb = sinon.spy();
        const storages = [
            {id:1, storageType: "Local", rootpath: "/path1"},
            {id:2, storageType: "Local", rootpath: "/path2"},

        ];
        const rendered = shallow(<StorageSelector enabled={true} selectedStorage={1} selectionUpdated={updateCb} storageList={storages}/>);

        rendered.find('select').simulate('change', {target: {value: "2"}});
        assert(updateCb.calledWith(2));
    });
});