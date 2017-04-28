import React from 'react';
import axios from 'axios';
import SortableTable from 'react-sortable-table';

class GeneralListComponent extends React.Component {
    constructor(props){
        super(props);
        this.state = {
            data: [],
            hovered: false
        };

        this.gotDataCallback = this.gotDataCallback.bind(this);

        /* this must be supplied by a subclass */
        this.endpoint='/unknown';

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

    /* reloads the data for the component based on the endpoint configured in the constructor */
    reload(){
        const component = this;

        axios.get(this.endpoint).then(component.gotDataCallback).catch(function (error) {
            console.error(error);
        });
    }

    /* called when we receive data; can be over-ridden by a subclass to do something more clever */
    gotDataCallback(result){
        this.setState({
            data: result.data.result
        });
    }

    render() {
        return (
            <SortableTable
                data={ this.state.data}
                columns={this.columns}
                style={this.style}
                iconStyle={this.iconStyle}
                tableProps={ {className: "dashboardpanel"} }
            />
        );
    }
}

export default GeneralListComponent;