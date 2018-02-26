import React from 'react';
import axios from 'axios';
import SortableTable from 'react-sortable-table';
import {Link} from 'react-router-dom';

class GeneralListComponent extends React.Component {
    constructor(props){
        super(props);
        this.state = {
            data: [],
            hovered: false,
            filterTerms: {
                match: "W_ENDSWITH"
            },
            currentPage: 0
        };

        this.pageSize = 20;

        this.gotDataCallback = this.gotDataCallback.bind(this);
        this.newElementCallback = this.newElementCallback.bind(this);
        this.filterDidUpdate = this.filterDidUpdate.bind(this);

        /* this must be supplied by a subclass */
        this.endpoint='/unknown';
        this.filterEndpoint = "/unknown.list";

        this.style = {
            backgroundColor: '#eee',
            border: '1px solid black',
            borderCollapse: 'collapse'
        };

        this.iconStyle = {
            color: '#aaa',
            paddingLeft: '5px',
            paddingRight: '5px'
        };

        this.canCreateNew=true;
        /* this must be supplied by a subclass */
        this.columns = [

        ];
    }

    componentDidMount() {
        this.reload();
    }

    /* this method supplies a column definition as a convenience for subclasses */
    static standardColumn(name, key) {
        return {
            header: name,
            key: key,
            headerProps: { className: 'dashboardheader'},
            render: (value)=><span style={{fontStyle: "italic"}}>n/a</span> ? value : (value && value.length>0)
        };
    }

    breakdownPathComponents() {
        console.log("breakdownPathComponents: " + this.props.location.pathname);
        return this.props.location.pathname.split('/')
    }

    /* this method supplies the edit and delete icons. Can't be static as <Link> relies on the object context to access
     * history. */
    actionIcons() {
        const componentName = this.breakdownPathComponents()[1];
        return {
            header: "",
            key: "id",
            render: (id) => <span className="icons">
                    <Link to={"/" + componentName + "/" + id}><img className="smallicon" src="/assets/images/edit.png"/></Link>
                    <Link to={"/" + componentName + "/" + id + "/delete"}><img className="smallicon" src="/assets/images/delete.png"/></Link>
            </span>
        }
    }

    /* loads the next page of data */
    getNextPage(){
        const startAt = this.state.currentPage * this.pageSize;
        const length = this.pageSize;
        console.log("getNextPage");

        axios.put(this.filterEndpoint + "?startAt=" + startAt + "&length=" + length, this.state.filterTerms)
            .then(response=>this.setState({
                currentPage: this.state.currentPage+1
            }, ()=>{
                this.gotDataCallback(response, ()=> {
                    if (response.data.result.length > 0) this.getNextPage()
                });
            }))
            .catch(error=>{
            console.error(error);
        });
    }

    /* reloads the data for the component based on the endpoint configured in the constructor */
    reload(){
        console.log("reload()");
        this.setState({
            currentPage: 0,
            data: []
        }, ()=> this.getNextPage());
    }

    /* called when we receive data; can be over-ridden by a subclass to do something more clever */
    gotDataCallback(response, cb){
        this.setState({
            data: this.state.data.concat(response.data.result)
        }, cb);
    }

    /* called when the New button is clicked; can be over-ridden by a subclass to do something more clever */
    newElementCallback(event){

    }

    /* called to insert a filtering component; should be over-ridden by a subclass if filtering is required */
    getFilterComponent(){
        return <span/>
    }

    /* this can be referenced from a filter component in a subclass and should be called to update the active filtering.
    this will cause a reload of data from the server
     */
    filterDidUpdate(newterms){
        console.log("filterDidUpdate");
        this.setState({filterTerms: newterms},()=>this.reload());
    }

    render() {
        return (
            <div>
                <span className="list-title"><h2 className="list-title">{this.props.title}</h2></span>
                {this.getFilterComponent()}
                <SortableTable
                    data={ this.state.data}
                    columns={this.columns}
                    style={this.style}
                    iconStyle={this.iconStyle}
                    tableProps={ {className: "dashboardpanel"} }
                />

                <span className="banner-control">
                    <button id="newElementButton" onClick={this.newElementCallback}>New</button>
                </span>
            </div>
        );
    }
}

export default GeneralListComponent;